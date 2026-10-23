package com.superfercho.assistant.application.dto.llm;

import java.util.List;

public record LlmResponse(String text, List<LlmMessage.LlmToolCall> toolCalls, LlmUsage usage) {

    public LlmResponse {
        text = text == null ? "" : text;
        toolCalls = List.copyOf(toolCalls == null ? List.of() : toolCalls);
    }

    public static LlmResponse text(String text) {
        return new LlmResponse(text, List.of(), null);
    }

    public static LlmResponse toolCalls(List<LlmMessage.LlmToolCall> toolCalls) {
        return new LlmResponse("", toolCalls, null);
    }

    public static LlmResponse withUsage(LlmResponse base, LlmUsage usage) {
        return new LlmResponse(base.text, base.toolCalls, usage);
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
