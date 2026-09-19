package com.superfercho.assistant.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpenAiChatRequest(String model, List<OpenAiChatMessage> messages, List<OpenAiTool> tools) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpenAiChatMessage(
        String role,
        String content,
        @JsonProperty("tool_calls") List<OpenAiToolCall> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId,
        String name) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpenAiToolCall(String id, String type, OpenAiFunctionCall function) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpenAiFunctionCall(String name, String arguments) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpenAiTool(String type, OpenAiFunctionDefinition function) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpenAiFunctionDefinition(String name, String description, OpenAiParameters parameters) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpenAiParameters(String type, Map<String, OpenAiJsonSchemaProperty> properties, List<String> required) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpenAiJsonSchemaProperty(String type, String description) {}
