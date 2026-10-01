package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AdminNewCustomersResult;
import com.superfercho.identity.application.dto.CustomerRegistrationBucketRow;
import com.superfercho.identity.application.dto.GetAdminNewCustomersCommand;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.platform.time.BucketGranularity;
import com.superfercho.platform.time.BusinessZone;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * New customers for an arbitrary [from, to) period. Based on the real
 * CustomerRecord creation instant (registration), not first purchase; previews
 * and reused commercial records are not counted. Aggregation happens in the
 * database; empty buckets are filled for chart continuity. Bucket boundaries
 * follow America/Bogota.
 */
public final class GetAdminNewCustomersUseCase {

    private static final ZoneId ZONE = BusinessZone.BOGOTA;
    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter HOUR_LABEL =
            DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-CO"));
    private static final int MAX_BUCKETS = 500;

    private final CustomerRecordRepository customerRecordRepository;

    public GetAdminNewCustomersUseCase(CustomerRecordRepository customerRecordRepository) {
        this.customerRecordRepository = customerRecordRepository;
    }

    public AdminNewCustomersResult execute(GetAdminNewCustomersCommand command) {
        Objects.requireNonNull(command, "command");
        long total = customerRecordRepository.countRegistrationsBetween(command.from(), command.to());
        Map<Instant, CustomerRegistrationBucketRow> rowsByBucket = new HashMap<>();
        for (CustomerRegistrationBucketRow row : customerRecordRepository.countRegistrationsByBucket(
                command.granularity(), command.from(), command.to())) {
            rowsByBucket.put(row.bucketStart(), row);
        }

        List<AdminNewCustomersResult.AdminNewCustomersBucket> buckets = new ArrayList<>();
        Instant cursor = bucketStart(command.granularity(), command.from());
        while (cursor.isBefore(command.to())) {
            CustomerRegistrationBucketRow row = rowsByBucket.get(cursor);
            buckets.add(new AdminNewCustomersResult.AdminNewCustomersBucket(
                    cursor, labelFor(command.granularity(), cursor), row == null ? 0 : row.count()));
            cursor = advance(command.granularity(), cursor);
            if (buckets.size() > MAX_BUCKETS) {
                throw new IllegalStateException("Customer registration bucket generation overflow");
            }
        }
        return new AdminNewCustomersResult(total, List.copyOf(buckets));
    }

    private static Instant bucketStart(BucketGranularity granularity, Instant instant) {
        return switch (granularity) {
            case HOUR -> instant.truncatedTo(ChronoUnit.HOURS);
            case DAY -> LocalDate.ofInstant(instant, ZONE).atStartOfDay(ZONE).toInstant();
            case MONTH -> YearMonth.from(LocalDate.ofInstant(instant, ZONE))
                    .atDay(1)
                    .atStartOfDay(ZONE)
                    .toInstant();
        };
    }

    private static Instant advance(BucketGranularity granularity, Instant cursor) {
        return switch (granularity) {
            case HOUR -> cursor.plus(1, ChronoUnit.HOURS);
            case DAY -> cursor.plus(1, ChronoUnit.DAYS);
            case MONTH -> YearMonth.from(LocalDate.ofInstant(cursor, ZONE))
                    .plusMonths(1)
                    .atDay(1)
                    .atStartOfDay(ZONE)
                    .toInstant();
        };
    }

    private static String labelFor(BucketGranularity granularity, Instant periodStart) {
        return switch (granularity) {
            case HOUR -> HOUR_LABEL.format(periodStart.atZone(ZONE));
            case DAY -> DAY_LABEL.format(periodStart.atZone(ZONE));
            case MONTH -> MONTH_LABEL.format(periodStart.atZone(ZONE));
        };
    }
}
