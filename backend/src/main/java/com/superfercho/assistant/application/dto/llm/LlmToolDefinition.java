package com.superfercho.assistant.application.dto.llm;

import java.util.List;

public record LlmToolDefinition(String name, String description, List<LlmToolParameter> parameters) {

    public LlmToolDefinition {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    public record LlmToolParameter(String name, String type, boolean required, String description) {}
}
