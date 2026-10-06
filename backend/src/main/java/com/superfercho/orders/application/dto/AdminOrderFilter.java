package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.model.OrderStatus;
import java.time.Instant;
import java.util.List;

/**
 * Optional filters for the admin orders list. Empty {@code statuses} means no
 * status restriction; null {@code orderNumberContains} means no search; null
 * bounds mean unbounded. Bounds follow {@code [fromInclusive, toExclusive)};
 * range validation happens in {@link ListAdminOrdersCommand}.
 */
public record AdminOrderFilter(
        List<OrderStatus> statuses, String orderNumberContains, Instant fromInclusive, Instant toExclusive) {

    public AdminOrderFilter {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
        if (orderNumberContains != null && orderNumberContains.isBlank()) {
            orderNumberContains = null;
        }
    }

    public boolean hasStatuses() {
        return !statuses.isEmpty();
    }

    public boolean hasSearch() {
        return orderNumberContains != null;
    }

    public boolean hasRange() {
        return fromInclusive != null || toExclusive != null;
    }
}
