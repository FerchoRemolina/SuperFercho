package com.superfercho.platform.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Business time zone for SuperFercho periods. Calendar periods (day, week, month, year)
 * are defined in Colombia local time, not UTC.
 */
public final class BusinessZone {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private BusinessZone() {}

    /**
     * Parses a range bound accepting ISO-8601 instants with offset
     * ({@code 2026-05-01T05:00:00Z}, {@code 2026-05-01T00:00:00-05:00}),
     * naive local date-times interpreted in {@link #BOGOTA}
     * ({@code 2026-05-01T00:00:00}) and bare dates interpreted as Bogota
     * midnight ({@code 2026-05-01}).
     */
    public static java.time.Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DateTimeParseException("blank instant", raw == null ? "" : raw, 0);
        }
        String value = raw.trim();
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(value).atZone(BOGOTA).toInstant();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDate.parse(value).atStartOfDay(BOGOTA).toInstant();
        } catch (DateTimeParseException ex) {
            throw new DateTimeParseException("Unsupported instant: " + value, value, 0, ex);
        }
    }
}
