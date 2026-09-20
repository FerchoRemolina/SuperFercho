package com.superfercho.assistant.application.tool;

import java.util.List;

public record ToolSchema(List<ToolParameter> parameters) {

    public ToolSchema {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    public static ToolSchema of(ToolParameter... parameters) {
        return new ToolSchema(List.of(parameters));
    }
}
