package com.superfercho.assistant.application.dto.llm;

import java.util.List;

public record LlmRequest(List<LlmMessage> messages, List<LlmToolDefinition> tools) {

    public LlmRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }
}
