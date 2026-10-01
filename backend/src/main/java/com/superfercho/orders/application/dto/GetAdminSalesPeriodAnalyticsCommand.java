package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.exception.InvalidSalesPeriodException;
import com.superfercho.platform.time.BucketGranularity;
import java.time.Instant;
import java.util.Locale;

/**
 * Arbitrary [from, to) sales analytics period. When from/to are provided the
 * granularity is a bucket size (HOUR, DAY or MONTH); it defaults to DAY.
 */
public record GetAdminSalesPeriodAnalyticsCommand(
        Instant from, Instant to, BucketGranularity granularity) {

    public GetAdminSalesPeriodAnalyticsCommand {
        DashboardPeriods.requireValidBucketRange(granularity, from, to);
    }

    public static GetAdminSalesPeriodAnalyticsCommand of(String rawFrom, String rawTo, String rawGranularity) {
        Instant from = DashboardPeriods.parseBound(rawFrom, "from");
        Instant to = DashboardPeriods.parseBound(rawTo, "to");
        return new GetAdminSalesPeriodAnalyticsCommand(from, to, parseGranularity(rawGranularity));
    }

    private static BucketGranularity parseGranularity(String raw) {
        if (raw == null || raw.isBlank()) {
            return BucketGranularity.DAY;
        }
        try {
            return BucketGranularity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidSalesPeriodException("Unsupported sales granularity: " + raw);
        }
    }
}
