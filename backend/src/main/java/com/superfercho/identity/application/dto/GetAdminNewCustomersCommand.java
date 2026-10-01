package com.superfercho.identity.application.dto;

import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;
import com.superfercho.platform.time.BucketGranularity;
import com.superfercho.platform.time.BusinessZone;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Arbitrary [from, to) period for the new-customers metric. Counts are based on
 * the real CustomerRecord creation instant, bucketed in America/Bogota.
 * Granularity HOUR|DAY|MONTH, default DAY. Ranges are bounded: 7 days for HOUR,
 * 400 days for DAY, 5 years overall.
 */
public record GetAdminNewCustomersCommand(Instant from, Instant to, BucketGranularity granularity) {

    private static final Duration MAX_RANGE = Duration.ofDays(1826);
    private static final Duration MAX_HOUR_RANGE = Duration.ofDays(7);
    private static final Duration MAX_DAY_RANGE = Duration.ofDays(400);

    public GetAdminNewCustomersCommand {
        requireValidBucketRange(granularity, from, to);
    }

    public static GetAdminNewCustomersCommand of(String rawFrom, String rawTo, String rawGranularity) {
        Instant from = parseBound(rawFrom, "from");
        Instant to = parseBound(rawTo, "to");
        return new GetAdminNewCustomersCommand(from, to, parseGranularity(rawGranularity));
    }

    private static Instant parseBound(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidAdminCustomerQueryException(field + " is required");
        }
        try {
            return BusinessZone.parseInstant(raw);
        } catch (DateTimeParseException ex) {
            throw new InvalidAdminCustomerQueryException(
                    field + " must be a valid ISO-8601 date or date-time: " + raw);
        }
    }

    private static BucketGranularity parseGranularity(String raw) {
        if (raw == null || raw.isBlank()) {
            return BucketGranularity.DAY;
        }
        try {
            return BucketGranularity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidAdminCustomerQueryException("Unsupported granularity: " + raw);
        }
    }

    private static void requireValidBucketRange(BucketGranularity granularity, Instant from, Instant to) {
        if (from == null || to == null) {
            throw new InvalidAdminCustomerQueryException("from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new InvalidAdminCustomerQueryException("from must be before to");
        }
        Duration span = Duration.between(from, to);
        if (span.compareTo(MAX_RANGE) > 0) {
            throw new InvalidAdminCustomerQueryException(
                    "period must not exceed " + MAX_RANGE.toDays() + " days");
        }
        Duration max = switch (granularity) {
            case HOUR -> MAX_HOUR_RANGE;
            case DAY -> MAX_DAY_RANGE;
            case MONTH -> MAX_RANGE;
        };
        if (span.compareTo(max) > 0) {
            throw new InvalidAdminCustomerQueryException(
                    granularity + " granularity supports periods up to " + max.toDays() + " days");
        }
    }
}
