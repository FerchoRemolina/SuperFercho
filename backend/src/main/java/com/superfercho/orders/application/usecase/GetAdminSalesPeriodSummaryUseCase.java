package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.AdminSalesBucketResult;
import com.superfercho.orders.application.dto.AdminSalesPeriodSummaryResult;
import com.superfercho.orders.application.dto.GetAdminSalesPeriodSummaryCommand;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.SalesPeriodGranularity;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * LEGACY rolling-window sales summary (no from/to on the request). The Admin UI
 * no longer consumes it (it uses the [from, to) analytics endpoint), but the
 * contract keeps the current business rule: a sale is recognized only when the
 * order is DELIVERED, anchored on {@code delivered_at}. Window edges remain in
 * UTC for backwards compatibility.
 */
public final class GetAdminSalesPeriodSummaryUseCase {

    private static final ZoneOffset ZONE = ZoneOffset.UTC;
    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter HOUR_LABEL =
            DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-CO"));

    private final OrderRepository orderRepository;
    private final ClockProvider clockProvider;

    public GetAdminSalesPeriodSummaryUseCase(OrderRepository orderRepository, ClockProvider clockProvider) {
        this.orderRepository = orderRepository;
        this.clockProvider = clockProvider;
    }

    public AdminSalesPeriodSummaryResult execute(GetAdminSalesPeriodSummaryCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = clockProvider.currentTime();
        Range range = rangeFor(command.granularity(), now);
        List<Order> orders =
                orderRepository.findDeliveredBetween(range.fromInclusive(), range.toExclusive());

        Map<Instant, Accumulator> buckets = emptyBuckets(command.granularity(), range, now);
        for (Order order : orders) {
            Instant key = bucketKey(command.granularity(), order.deliveredAt());
            Accumulator accumulator = buckets.get(key);
            if (accumulator == null) {
                continue;
            }
            accumulator.add(order.total().amount());
        }

        List<AdminSalesBucketResult> points = new ArrayList<>(buckets.size());
        for (Map.Entry<Instant, Accumulator> entry : buckets.entrySet()) {
            points.add(new AdminSalesBucketResult(
                    entry.getKey(),
                    labelFor(command.granularity(), entry.getKey()),
                    Money.cop(entry.getValue().total),
                    entry.getValue().orderCount));
        }
        points.sort(Comparator.comparing(AdminSalesBucketResult::periodStart));
        return new AdminSalesPeriodSummaryResult(command.granularity(), List.copyOf(points));
    }

    private static Range rangeFor(SalesPeriodGranularity granularity, Instant now) {
        return switch (granularity) {
            case DAY -> {
                Instant to = now.truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS);
                yield new Range(to.minus(24, ChronoUnit.HOURS), to);
            }
            case WEEK -> {
                LocalDate end = LocalDate.ofInstant(now, ZONE).plusDays(1);
                yield new Range(
                        end.minusDays(7).atStartOfDay().toInstant(ZONE),
                        end.atStartOfDay().toInstant(ZONE));
            }
            case MONTH -> {
                LocalDate end = LocalDate.ofInstant(now, ZONE).plusDays(1);
                yield new Range(
                        end.minusDays(30).atStartOfDay().toInstant(ZONE),
                        end.atStartOfDay().toInstant(ZONE));
            }
            case YEAR -> {
                YearMonth end = YearMonth.from(LocalDate.ofInstant(now, ZONE)).plusMonths(1);
                yield new Range(
                        end.minusMonths(12).atDay(1).atStartOfDay().toInstant(ZONE),
                        end.atDay(1).atStartOfDay().toInstant(ZONE));
            }
        };
    }

    private static Map<Instant, Accumulator> emptyBuckets(
            SalesPeriodGranularity granularity, Range range, Instant now) {
        Map<Instant, Accumulator> buckets = new LinkedHashMap<>();
        Instant cursor = range.fromInclusive();
        while (cursor.isBefore(range.toExclusive())) {
            buckets.put(cursor, new Accumulator());
            cursor = switch (granularity) {
                case DAY -> cursor.plus(1, ChronoUnit.HOURS);
                case WEEK, MONTH -> cursor.plus(1, ChronoUnit.DAYS);
                case YEAR -> YearMonth.from(LocalDate.ofInstant(cursor, ZONE))
                        .plusMonths(1)
                        .atDay(1)
                        .atStartOfDay()
                        .toInstant(ZONE);
            };
            if (buckets.size() > 400) {
                throw new IllegalStateException("Sales bucket generation overflow");
            }
        }
        // Ensure current partial bucket exists for DAY when now falls mid-hour already truncated.
        Instant currentKey = bucketKey(granularity, now);
        buckets.putIfAbsent(currentKey, new Accumulator());
        return buckets;
    }

    private static Instant bucketKey(SalesPeriodGranularity granularity, Instant instant) {
        return switch (granularity) {
            case DAY -> instant.truncatedTo(ChronoUnit.HOURS);
            case WEEK, MONTH -> LocalDate.ofInstant(instant, ZONE).atStartOfDay().toInstant(ZONE);
            case YEAR -> YearMonth.from(LocalDate.ofInstant(instant, ZONE))
                    .atDay(1)
                    .atStartOfDay()
                    .toInstant(ZONE);
        };
    }

    private static String labelFor(SalesPeriodGranularity granularity, Instant periodStart) {
        return switch (granularity) {
            case DAY -> HOUR_LABEL.format(periodStart.atZone(ZONE));
            case WEEK, MONTH -> DAY_LABEL.format(periodStart.atZone(ZONE));
            case YEAR -> MONTH_LABEL.format(periodStart.atZone(ZONE));
        };
    }

    private record Range(Instant fromInclusive, Instant toExclusive) {}

    private static final class Accumulator {
        private BigDecimal total = BigDecimal.ZERO.setScale(2);
        private long orderCount;

        void add(BigDecimal amount) {
            total = total.add(amount);
            orderCount++;
        }
    }
}
