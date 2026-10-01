package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.exception.InvalidSalesPeriodException;
import com.superfercho.platform.time.BucketGranularity;
import com.superfercho.platform.time.BusinessZone;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Shared validation for admin dashboard [from, to) periods. Bounds are ISO-8601
 * instants; naive date-times and bare dates are interpreted in America/Bogota.
 */
final class DashboardPeriods {

    /** Maximum span accepted on any dashboard endpoint (5 years). */
    static final Duration MAX_RANGE = Duration.ofDays(1826);
    /** HOUR buckets are bounded to one week (<= 169 buckets). */
    private static final Duration MAX_HOUR_RANGE = Duration.ofDays(7);
    /** DAY buckets are bounded to 400 days. */
    private static final Duration MAX_DAY_RANGE = Duration.ofDays(400);

    private DashboardPeriods() {}

    static Instant parseBound(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidSalesPeriodException(field + " is required");
        }
        try {
            return BusinessZone.parseInstant(raw);
        } catch (DateTimeParseException ex) {
            throw new InvalidSalesPeriodException(field + " must be a valid ISO-8601 date or date-time: " + raw);
        }
    }

    static void requireValidRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new InvalidSalesPeriodException("from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new InvalidSalesPeriodException("from must be before to");
        }
        if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw new InvalidSalesPeriodException("period must not exceed " + MAX_RANGE.toDays() + " days");
        }
    }

    static void requireValidBucketRange(BucketGranularity granularity, Instant from, Instant to) {
        requireValidRange(from, to);
        Duration span = Duration.between(from, to);
        Duration max = switch (granularity) {
            case HOUR -> MAX_HOUR_RANGE;
            case DAY -> MAX_DAY_RANGE;
            case MONTH -> MAX_RANGE;
        };
        if (span.compareTo(max) > 0) {
            throw new InvalidSalesPeriodException(
                    granularity + " granularity supports periods up to " + max.toDays() + " days");
        }
    }
}
