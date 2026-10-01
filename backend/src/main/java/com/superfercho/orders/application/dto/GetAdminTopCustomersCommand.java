package com.superfercho.orders.application.dto;

import java.time.Instant;

/**
 * Arbitrary [from, to) period for the top customers ranking, ordered by total
 * purchased value (CANCELLED orders excluded).
 */
public record GetAdminTopCustomersCommand(Instant from, Instant to, int limit) {

    public static final int DEFAULT_LIMIT = 5;
    public static final int MAX_LIMIT = 50;

    public GetAdminTopCustomersCommand {
        DashboardPeriods.requireValidRange(from, to);
    }

    public static GetAdminTopCustomersCommand of(String rawFrom, String rawTo, Integer limit) {
        return new GetAdminTopCustomersCommand(
                DashboardPeriods.parseBound(rawFrom, "from"),
                DashboardPeriods.parseBound(rawTo, "to"),
                normalizeLimit(limit));
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
