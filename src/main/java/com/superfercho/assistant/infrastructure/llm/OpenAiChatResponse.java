package com.superfercho.assistant.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiChatResponse(List<OpenAiChoice> choices) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiChoice(OpenAiResponseMessage message) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiResponseMessage(
        String role, String content, @JsonProperty("tool_calls") List<OpenAiResponseToolCall> toolCalls) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiResponseToolCall(String id, String type, OpenAiResponseFunction function) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiResponseFunction(String name, String arguments) {}
