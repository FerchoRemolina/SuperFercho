package com.superfercho.assistant.application.dto.llm;

import java.util.List;

public record LlmResponse(String text, List<LlmMessage.LlmToolCall> toolCalls) {

    public LlmResponse {
        text = text == null ? "" : text;
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static LlmResponse text(String text) {
        return new LlmResponse(text, List.of());
    }

    public static LlmResponse toolCalls(List<LlmMessage.LlmToolCall> toolCalls) {
        return new LlmResponse("", List.copyOf(toolCalls));
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
