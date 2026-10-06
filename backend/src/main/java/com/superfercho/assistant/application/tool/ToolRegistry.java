package com.superfercho.assistant.application.tool;

import com.superfercho.assistant.application.dto.llm.LlmToolDefinition;
import com.superfercho.assistant.application.exception.InvalidToolArgumentsException;
import com.superfercho.assistant.application.exception.ToolAccessDeniedException;
import com.superfercho.assistant.application.exception.ToolNotAllowedException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ToolRegistry {

    private final Map<String, AssistantTool> toolsByName;

    public ToolRegistry(List<AssistantTool> tools) {
        Map<String, AssistantTool> indexed = new LinkedHashMap<>();
        for (AssistantTool tool : tools) {
            indexed.put(tool.name(), tool);
        }
        this.toolsByName = Map.copyOf(indexed);
    }

    public List<AssistantTool> allowlist() {
        return List.copyOf(toolsByName.values());
    }

    public List<AssistantTool> allowlistFor(ToolAccess callerAccess) {
        List<AssistantTool> allowed = new ArrayList<>();
        for (AssistantTool tool : toolsByName.values()) {
            if (callerAccess.covers(tool.access())) {
                allowed.add(tool);
            }
        }
        return List.copyOf(allowed);
    }

    public List<LlmToolDefinition> definitionsFor(ToolAccess callerAccess) {
        return allowlistFor(callerAccess).stream().map(AssistantTool::definition).toList();
    }

    public AssistantTool requireAllowed(String name) {
        if (name == null || name.isBlank()) {
            throw new ToolNotAllowedException(String.valueOf(name));
        }
        AssistantTool tool = toolsByName.get(name);
        if (tool == null) {
            throw new ToolNotAllowedException(name);
        }
        return tool;
    }

    public AssistantTool requireAllowedFor(ToolAccess callerAccess, String name) {
        AssistantTool tool = requireAllowed(name);
        if (!callerAccess.covers(tool.access())) {
            throw new ToolAccessDeniedException(name);
        }
        return tool;
    }

    public ToolResult execute(String name, Map<String, Object> rawArguments) {
        return execute(ToolAccess.CUSTOMER, name, rawArguments);
    }

    public ToolResult execute(ToolAccess callerAccess, String name, Map<String, Object> rawArguments) {
        AssistantTool tool = requireAllowedFor(callerAccess, name);
        ToolArguments arguments = ToolArguments.of(rawArguments);
        Set<String> allowed = tool.schema().parameters().stream()
                .map(ToolParameter::name)
                .collect(Collectors.toSet());
        arguments.rejectUnknown(allowed);
        for (ToolParameter parameter : tool.schema().parameters()) {
            if (parameter.required() && !arguments.asMap().containsKey(parameter.name())) {
                throw new InvalidToolArgumentsException(parameter.name() + " is required");
            }
        }
        try {
            return tool.execute(arguments);
        } catch (InvalidToolArgumentsException | ToolNotAllowedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            return ToolResult.failure(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }
}
