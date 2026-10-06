package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.exception.InvalidSalesPeriodException;
import java.time.Instant;
import java.util.Locale;

/**
 * Arbitrary [from, to) period for the top customers ranking (CANCELLED excluded).
 * Default sort is TOTAL spent; ORDERS ranks by non-cancelled order count.
 */
public record GetAdminTopCustomersCommand(
        Instant from, Instant to, int limit, SortBy sortBy) {

    public static final int DEFAULT_LIMIT = 5;
    public static final int MAX_LIMIT = 50;

    public enum SortBy {
        TOTAL,
        ORDERS;

        public static SortBy parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return TOTAL;
            }
            try {
                return SortBy.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new InvalidSalesPeriodException("Unsupported top customers sort: " + raw);
            }
        }
    }

    public GetAdminTopCustomersCommand {
        DashboardPeriods.requireValidRange(from, to);
        if (sortBy == null) {
            sortBy = SortBy.TOTAL;
        }
    }

    public static GetAdminTopCustomersCommand of(String rawFrom, String rawTo, Integer limit) {
        return of(rawFrom, rawTo, limit, null);
    }

    public static GetAdminTopCustomersCommand of(
            String rawFrom, String rawTo, Integer limit, String rawSort) {
        return new GetAdminTopCustomersCommand(
                DashboardPeriods.parseBound(rawFrom, "from"),
                DashboardPeriods.parseBound(rawTo, "to"),
                normalizeLimit(limit),
                SortBy.parse(rawSort));
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
