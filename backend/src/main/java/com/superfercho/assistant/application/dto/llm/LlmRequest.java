package com.superfercho.assistant.application.dto.llm;

import java.util.List;

public record LlmRequest(List<LlmMessage> messages, List<LlmToolDefinition> tools, String systemPrompt) {

    public LlmRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
        systemPrompt = systemPrompt == null || systemPrompt.isBlank() ? null : systemPrompt;
    }

    public LlmRequest(List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        this(messages, tools, null);
    }
}
