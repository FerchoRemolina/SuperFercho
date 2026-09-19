package com.superfercho.assistant.application.tool;

import com.superfercho.assistant.application.exception.InvalidToolArgumentsException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ToolArguments {

    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "userid", "customerid", "user_id", "customer_id", "catalogview", "view");

    private final Map<String, Object> values;

    private ToolArguments(Map<String, Object> values) {
        this.values = values;
    }

    public static ToolArguments of(Map<String, Object> raw) {
        Map<String, Object> values = raw == null ? Map.of() : Map.copyOf(raw);
        for (String key : values.keySet()) {
            if (key != null && FORBIDDEN_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                throw new InvalidToolArgumentsException("argument is not allowed: " + key);
            }
        }
        return new ToolArguments(values);
    }

    public void rejectUnknown(Set<String> allowed) {
        for (String key : values.keySet()) {
            if (!allowed.contains(key)) {
                throw new InvalidToolArgumentsException("unknown argument: " + key);
            }
        }
    }

    public UUID requireUuid(String name) {
        Object value = require(name);
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException exception) {
            throw new InvalidToolArgumentsException(name + " must be a UUID");
        }
    }

    public Optional<UUID> optionalUuid(String name) {
        if (!values.containsKey(name) || values.get(name) == null) {
            return Optional.empty();
        }
        return Optional.of(requireUuid(name));
    }

    public String requireText(String name) {
        Object value = require(name);
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            throw new InvalidToolArgumentsException(name + " cannot be blank");
        }
        return text;
    }

    public Optional<String> optionalText(String name) {
        if (!values.containsKey(name) || values.get(name) == null) {
            return Optional.empty();
        }
        return Optional.of(requireText(name));
    }

    public int requirePositiveInt(String name) {
        int value = requireInt(name);
        if (value <= 0) {
            throw new InvalidToolArgumentsException(name + " must be greater than 0");
        }
        return value;
    }

    public Optional<Integer> optionalInt(String name) {
        if (!values.containsKey(name) || values.get(name) == null) {
            return Optional.empty();
        }
        return Optional.of(requireInt(name));
    }

    public Optional<Integer> optionalPositiveInt(String name) {
        if (!values.containsKey(name) || values.get(name) == null) {
            return Optional.empty();
        }
        return Optional.of(requirePositiveInt(name));
    }

    public Money requireMoney(String name) {
        Object value = require(name);
        try {
            return Money.cop(new BigDecimal(String.valueOf(value)));
        } catch (RuntimeException exception) {
            throw new InvalidToolArgumentsException(name + " must be a COP amount");
        }
    }

    public Map<String, Object> asMap() {
        return values;
    }

    public String render() {
        return new LinkedHashMap<>(values).toString();
    }

    private int requireInt(String name) {
        Object value = require(name);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new InvalidToolArgumentsException(name + " must be an integer");
        }
    }

    private Object require(String name) {
        if (!values.containsKey(name) || values.get(name) == null) {
            throw new InvalidToolArgumentsException(name + " is required");
        }
        return values.get(name);
    }
}
