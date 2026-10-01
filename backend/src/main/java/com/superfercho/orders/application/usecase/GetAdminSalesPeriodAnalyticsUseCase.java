package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.AdminSalesAnalyticsResult;
import com.superfercho.orders.application.dto.AdminSalesBucketResult;
import com.superfercho.orders.application.dto.AdminSalesBucketRow;
import com.superfercho.orders.application.dto.GetAdminSalesPeriodAnalyticsCommand;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.platform.money.Money;
import com.superfercho.platform.time.BucketGranularity;
import java.math.BigDecimal;
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
 * Sales series for an arbitrary [from, to) period. Aggregation happens in the
 * database; empty buckets are filled for chart continuity. CANCELLED orders are
 * excluded (same semantics as the rolling summary). Bucket boundaries follow
 * America/Bogota.
 */
public final class GetAdminSalesPeriodAnalyticsUseCase {

    private static final ZoneId ZONE = com.superfercho.platform.time.BusinessZone.BOGOTA;
    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter HOUR_LABEL =
            DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-CO"));
    private static final int MAX_BUCKETS = 500;

    private final OrderRepository orderRepository;

    public GetAdminSalesPeriodAnalyticsUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public AdminSalesAnalyticsResult execute(GetAdminSalesPeriodAnalyticsCommand command) {
        Objects.requireNonNull(command, "command");
        List<AdminSalesBucketRow> rows = orderRepository.aggregateSalesBuckets(
                command.granularity(), command.from(), command.to());
        Map<Instant, AdminSalesBucketRow> rowsByBucket = new HashMap<>();
        for (AdminSalesBucketRow row : rows) {
            rowsByBucket.put(row.bucketStart(), row);
        }

        List<AdminSalesBucketResult> buckets = new ArrayList<>();
        Instant cursor = bucketStart(command.granularity(), command.from());
        while (cursor.isBefore(command.to())) {
            AdminSalesBucketRow row = rowsByBucket.get(cursor);
            buckets.add(new AdminSalesBucketResult(
                    cursor,
                    labelFor(command.granularity(), cursor),
                    Money.cop(row == null ? BigDecimal.ZERO.setScale(2) : row.totalAmount()),
                    row == null ? 0 : row.orderCount()));
            cursor = advance(command.granularity(), cursor);
            if (buckets.size() > MAX_BUCKETS) {
                throw new IllegalStateException("Sales bucket generation overflow");
            }
        }
        return new AdminSalesAnalyticsResult(command.granularity(), List.copyOf(buckets));
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
