package com.superfercho.orders.application.dto;

import java.time.Instant;
import java.util.Locale;

/**
 * Arbitrary [from, to) period for the product units ranking. Units sold per
 * product exclude CANCELLED orders. {@code ascending=false} returns the best
 * sellers, {@code ascending=true} the least sold.
 */
public record GetAdminTopProductsCommand(Instant from, Instant to, int limit, boolean ascending) {

    public static final int DEFAULT_LIMIT = 5;
    public static final int MAX_LIMIT = 50;

    public GetAdminTopProductsCommand {
        DashboardPeriods.requireValidRange(from, to);
    }

    public static GetAdminTopProductsCommand of(String rawFrom, String rawTo, Integer limit, String sort) {
        return new GetAdminTopProductsCommand(
                DashboardPeriods.parseBound(rawFrom, "from"),
                DashboardPeriods.parseBound(rawTo, "to"),
                normalizeLimit(limit),
                parseSort(sort));
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private static boolean parseSort(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "DESC" -> false;
            case "ASC" -> true;
            default -> throw new com.superfercho.orders.domain.exception.InvalidSalesPeriodException(
                    "Unsupported product sort: " + raw);
        };
    }
}
