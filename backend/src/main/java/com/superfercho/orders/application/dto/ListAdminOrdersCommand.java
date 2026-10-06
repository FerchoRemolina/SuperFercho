package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.exception.InvalidOrderException;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.time.BusinessZone;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin orders list query. {@code from}/{@code to} are optional bounds of the
 * creation range {@code [fromInclusive, toExclusive)}; bare dates and naive
 * date-times are interpreted in America/Bogota (see BusinessZone).
 * {@code rawOrderNumber} is a case-insensitive contains filter on the order
 * number. Range validation: when both bounds are present,
 * {@code from} must be strictly before {@code to}.
 */
public record ListAdminOrdersCommand(
        Integer page,
        Integer size,
        List<OrderStatus> statuses,
        String orderNumber,
        Instant fromInclusive,
        Instant toExclusive) {

    public ListAdminOrdersCommand {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
        orderNumber = orderNumber == null || orderNumber.isBlank() ? null : orderNumber.trim();
        if (fromInclusive != null
                && toExclusive != null
                && !fromInclusive.isBefore(toExclusive)) {
            throw new InvalidOrderException("from must be before to");
        }
    }

    public boolean hasStatusFilter() {
        return !statuses.isEmpty();
    }

    public static ListAdminOrdersCommand of(Integer page, Integer size, List<String> rawStatuses) {
        return of(page, size, rawStatuses, null, null, null);
    }

    public static ListAdminOrdersCommand of(
            Integer page,
            Integer size,
            List<String> rawStatuses,
            String rawOrderNumber,
            String rawFrom,
            String rawTo) {
        return new ListAdminOrdersCommand(
                page, size, parseStatuses(rawStatuses), rawOrderNumber, parseBound(rawFrom, "from"),
                parseBound(rawTo, "to"));
    }

    private static List<OrderStatus> parseStatuses(List<String> rawStatuses) {
        if (rawStatuses == null || rawStatuses.isEmpty()) {
            return List.of();
        }
        Set<OrderStatus> parsed = new LinkedHashSet<>();
        for (String token : rawStatuses) {
            if (token == null || token.isBlank()) {
                continue;
            }
            for (String part : token.split(",")) {
                String value = part.trim();
                if (value.isEmpty()) {
                    continue;
                }
                try {
                    parsed.add(OrderStatus.valueOf(value));
                } catch (IllegalArgumentException exception) {
                    throw new InvalidOrderException("invalid order status: " + value);
                }
            }
        }
        return new ArrayList<>(parsed);
    }

    private static Instant parseBound(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return BusinessZone.parseInstant(raw);
        } catch (DateTimeParseException exception) {
            throw new InvalidOrderException(
                    field + " must be a valid ISO-8601 date or date-time: " + raw);
        }
    }
}
