package com.superfercho.assistant.application.tool;

import com.superfercho.assistant.application.exception.InvalidToolArgumentsException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.math.BigInteger;
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

    public int requireNonNegativeInt(String name) {
        int value = requireInt(name);
        if (value < 0) {
            throw new InvalidToolArgumentsException(name + " must be greater than or equal to 0");
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

    public BigDecimal requirePositiveDecimal(String name) {
        Object value = require(name);
        BigDecimal decimal;
        try {
            decimal = toFiniteDecimal(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidToolArgumentsException(name + " must be a finite decimal");
        }
        if (decimal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidToolArgumentsException(name + " must be greater than 0");
        }
        return decimal;
    }

    public String requireOneOf(String name, Set<String> allowed) {
        String text = requireText(name).toUpperCase(Locale.ROOT);
        if (!allowed.contains(text)) {
            throw new InvalidToolArgumentsException(name + " must be one of " + allowed);
        }
        return text;
    }

    public Map<String, Object> asMap() {
        return values;
    }

    public String render() {
        return new LinkedHashMap<>(values).toString();
    }

    private int requireInt(String name) {
        Object value = require(name);
        try {
            return toExactInt(value);
        } catch (ArithmeticException | IllegalArgumentException exception) {
            throw new InvalidToolArgumentsException(name + " must be an integer");
        }
    }

    private static int toExactInt(Object value) {
        if (value instanceof Boolean
                || value instanceof Map<?, ?>
                || value instanceof Iterable<?>
                || value instanceof UUID) {
            throw new IllegalArgumentException("not an integer");
        }
        BigDecimal decimal;
        if (value instanceof BigDecimal bigDecimal) {
            decimal = bigDecimal;
        } else if (value instanceof BigInteger bigInteger) {
            decimal = new BigDecimal(bigInteger);
        } else if (value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long) {
            decimal = BigDecimal.valueOf(((Number) value).longValue());
        } else if (value instanceof Float || value instanceof Double) {
            double number = ((Number) value).doubleValue();
            if (!Double.isFinite(number)) {
                throw new IllegalArgumentException("not an integer");
            }
            decimal = BigDecimal.valueOf(number);
        } else {
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) {
                throw new IllegalArgumentException("not an integer");
            }
            decimal = new BigDecimal(text);
        }
        return decimal.intValueExact();
    }

    private static BigDecimal toFiniteDecimal(Object value) {
        if (value instanceof Boolean
                || value instanceof Map<?, ?>
                || value instanceof Iterable<?>
                || value instanceof UUID) {
            throw new IllegalArgumentException("not a decimal");
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof BigInteger bigInteger) {
            return new BigDecimal(bigInteger);
        }
        if (value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        if (value instanceof Float || value instanceof Double) {
            double number = ((Number) value).doubleValue();
            if (!Double.isFinite(number)) {
                throw new IllegalArgumentException("not a decimal");
            }
            return BigDecimal.valueOf(number);
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("not a decimal");
        }
        return new BigDecimal(text);
    }

    private Object require(String name) {
        if (!values.containsKey(name) || values.get(name) == null) {
            throw new InvalidToolArgumentsException(name + " is required");
        }
        return values.get(name);
    }
}
