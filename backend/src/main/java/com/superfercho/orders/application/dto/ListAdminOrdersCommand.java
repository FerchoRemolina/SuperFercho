package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.exception.InvalidOrderException;
import com.superfercho.orders.domain.model.OrderStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ListAdminOrdersCommand(Integer page, Integer size, List<OrderStatus> statuses) {

    public ListAdminOrdersCommand {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
    }

    public static ListAdminOrdersCommand of(Integer page, Integer size, List<String> rawStatuses) {
        return new ListAdminOrdersCommand(page, size, parseStatuses(rawStatuses));
    }

    public boolean hasStatusFilter() {
        return !statuses.isEmpty();
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
}
