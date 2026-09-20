package com.superfercho.assistant.domain.model;

import com.superfercho.assistant.domain.exception.InvalidMessageException;
import java.util.Map;

public record MessageToolCall(String id, String name, Map<String, Object> arguments) {

    public MessageToolCall {
        if (id == null || id.isBlank()) {
            throw new InvalidMessageException("tool call id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidMessageException("tool call name cannot be null or blank");
        }
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
