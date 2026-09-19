package com.superfercho.assistant.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superfercho.assistant.application.dto.llm.LlmMessage;
import com.superfercho.assistant.application.dto.llm.LlmRequest;
import com.superfercho.assistant.application.dto.llm.LlmResponse;
import com.superfercho.assistant.application.dto.llm.LlmToolDefinition;
import com.superfercho.assistant.application.exception.LlmProviderException;
import com.superfercho.assistant.application.port.out.LLMPort;
import com.superfercho.assistant.domain.model.MessageRole;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public final class OpenAiChatAdapter implements LLMPort {

    private static final TypeReference<Map<String, Object>> ARGUMENTS_TYPE = new TypeReference<>() {};

    private final RestClient restClient;
    private final OpenAiChatProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiChatAdapter(RestClient restClient, OpenAiChatProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (request == null) {
            throw new LlmProviderException("llm request cannot be null");
        }
        if (!properties.hasApiKey()) {
            throw new LlmProviderException("OpenAI API key is not configured");
        }
        OpenAiChatRequest body = toProviderRequest(request);
        String raw;
        try {
            raw = restClient
                    .post()
                    .uri(properties.chatUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw translateHttpStatus(exception.getStatusCode().value());
        } catch (RestClientException exception) {
            if (isTimeout(exception)) {
                throw new LlmProviderException("llm provider request timed out");
            }
            throw new LlmProviderException("llm provider request failed");
        }
        return toLlmResponse(parseResponse(raw));
    }

    private OpenAiChatRequest toProviderRequest(LlmRequest request) {
        List<OpenAiChatMessage> messages = new ArrayList<>();
        for (LlmMessage message : request.messages()) {
            messages.add(toProviderMessage(message));
        }
        List<OpenAiTool> tools = request.tools().isEmpty() ? null : request.tools().stream()
                .map(this::toProviderTool)
                .toList();
        return new OpenAiChatRequest(properties.model(), List.copyOf(messages), tools);
    }

    private OpenAiChatMessage toProviderMessage(LlmMessage message) {
        if (message.role() == MessageRole.TOOL) {
            return new OpenAiChatMessage("tool", message.content(), null, message.toolCallId(), message.toolName());
        }
        if (message.role() == MessageRole.ASSISTANT && !message.toolCalls().isEmpty()) {
            List<OpenAiToolCall> toolCalls = new ArrayList<>();
            for (LlmMessage.LlmToolCall call : message.toolCalls()) {
                toolCalls.add(toProviderToolCall(call));
            }
            String content = message.content().isBlank() ? null : message.content();
            return new OpenAiChatMessage("assistant", content, List.copyOf(toolCalls), null, null);
        }
        String role = message.role() == MessageRole.USER ? "user" : "assistant";
        return new OpenAiChatMessage(role, message.content(), null, null, null);
    }

    private OpenAiToolCall toProviderToolCall(LlmMessage.LlmToolCall call) {
        try {
            String arguments = objectMapper.writeValueAsString(call.arguments());
            return new OpenAiToolCall(call.id(), "function", new OpenAiFunctionCall(call.name(), arguments));
        } catch (JsonProcessingException exception) {
            throw new LlmProviderException("llm provider request failed");
        }
    }

    private OpenAiTool toProviderTool(LlmToolDefinition definition) {
        Map<String, OpenAiJsonSchemaProperty> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (LlmToolDefinition.LlmToolParameter parameter : definition.parameters()) {
            properties.put(
                    parameter.name(),
                    new OpenAiJsonSchemaProperty(jsonSchemaType(parameter.type()), parameter.description()));
            if (parameter.required()) {
                required.add(parameter.name());
            }
        }
        OpenAiParameters parameters = new OpenAiParameters("object", Map.copyOf(properties), List.copyOf(required));
        return new OpenAiTool(
                "function",
                new OpenAiFunctionDefinition(definition.name(), definition.description(), parameters));
    }

    private OpenAiChatResponse parseResponse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LlmProviderException("llm provider returned an invalid result");
        }
        try {
            return objectMapper.readValue(raw, OpenAiChatResponse.class);
        } catch (JsonProcessingException exception) {
            throw new LlmProviderException("llm provider returned an invalid result");
        }
    }

    private LlmResponse toLlmResponse(OpenAiChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new LlmProviderException("llm provider returned an invalid result");
        }
        OpenAiChoice choice = response.choices().get(0);
        if (choice == null || choice.message() == null) {
            throw new LlmProviderException("llm provider returned an invalid result");
        }
        OpenAiResponseMessage message = choice.message();
        if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
            List<LlmMessage.LlmToolCall> calls = new ArrayList<>();
            for (OpenAiResponseToolCall toolCall : message.toolCalls()) {
                calls.add(toLlmToolCall(toolCall));
            }
            return LlmResponse.toolCalls(calls);
        }
        return LlmResponse.text(message.content() == null ? "" : message.content());
    }

    private LlmMessage.LlmToolCall toLlmToolCall(OpenAiResponseToolCall toolCall) {
        if (toolCall == null || toolCall.id() == null || toolCall.id().isBlank()) {
            throw new LlmProviderException("llm provider returned an invalid result");
        }
        if (toolCall.function() == null
                || toolCall.function().name() == null
                || toolCall.function().name().isBlank()) {
            throw new LlmProviderException("llm provider returned an invalid result");
        }
        return new LlmMessage.LlmToolCall(toolCall.id(), toolCall.function().name(), parseArguments(toolCall.function().arguments()));
    }

    private Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(arguments, ARGUMENTS_TYPE);
            if (parsed == null) {
                return Map.of();
            }
            if (parsed.containsValue(null)) {
                throw new LlmProviderException("llm provider returned an invalid result");
            }
            return Map.copyOf(parsed);
        } catch (JsonProcessingException exception) {
            throw new LlmProviderException("llm provider returned an invalid result");
        }
    }

    private static String jsonSchemaType(String type) {
        if (type == null || type.isBlank()) {
            return "string";
        }
        return switch (type) {
            case "string", "integer", "number", "boolean", "object", "array" -> type;
            default -> "string";
        };
    }

    private static LlmProviderException translateHttpStatus(int status) {
        if (status == 401 || status == 403) {
            return new LlmProviderException("llm provider authentication failed");
        }
        if (status == 429) {
            return new LlmProviderException("llm provider rate limit exceeded");
        }
        return new LlmProviderException("llm provider request failed");
    }

    private static boolean isTimeout(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
