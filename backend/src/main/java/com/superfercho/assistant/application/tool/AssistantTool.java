package com.superfercho.assistant.application.tool;

import com.superfercho.assistant.application.dto.llm.LlmToolDefinition;

public interface AssistantTool {

    String name();

    String description();

    ToolSchema schema();

    ToolRisk risk();

    ToolResult execute(ToolArguments arguments);

    default LlmToolDefinition definition() {
        return new LlmToolDefinition(
                name(),
                description(),
                schema().parameters().stream()
                        .map(parameter -> new LlmToolDefinition.LlmToolParameter(
                                parameter.name(), parameter.type(), parameter.required(), parameter.description()))
                        .toList());
    }
}
