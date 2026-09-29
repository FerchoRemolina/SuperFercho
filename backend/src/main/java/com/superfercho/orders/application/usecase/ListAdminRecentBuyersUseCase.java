package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.AdminRecentBuyerResult;
import com.superfercho.orders.application.dto.ListAdminRecentBuyersCommand;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Customers with non-cancelled purchases in the recent window, ordered by last purchase.
 */
public final class ListAdminRecentBuyersUseCase {

    private static final long LOOKBACK_DAYS = 30;

    private final OrderRepository orderRepository;
    private final ClockProvider clockProvider;

    public ListAdminRecentBuyersUseCase(OrderRepository orderRepository, ClockProvider clockProvider) {
        this.orderRepository = orderRepository;
        this.clockProvider = clockProvider;
    }

    public List<AdminRecentBuyerResult> execute(ListAdminRecentBuyersCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = clockProvider.currentTime();
        Instant from = now.minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
        List<Order> orders = orderRepository.findCreatedBetweenExcludingStatus(
                from, now.plus(1, ChronoUnit.SECONDS), OrderStatus.CANCELLED);

        Map<UUID, BuyerAccumulator> byCustomer = new HashMap<>();
        for (Order order : orders) {
            BuyerAccumulator accumulator = byCustomer.computeIfAbsent(
                    order.customerId(), ignored -> new BuyerAccumulator());
            accumulator.accept(order);
        }

        List<AdminRecentBuyerResult> results = new ArrayList<>(byCustomer.size());
        for (Map.Entry<UUID, BuyerAccumulator> entry : byCustomer.entrySet()) {
            BuyerAccumulator accumulator = entry.getValue();
            results.add(new AdminRecentBuyerResult(
                    entry.getKey(),
                    accumulator.displayName,
                    accumulator.lastOrderAt,
                    accumulator.orderCount,
                    accumulator.lastOrderTotal));
        }
        results.sort(Comparator.comparing(AdminRecentBuyerResult::lastOrderAt)
                .reversed()
                .thenComparing(AdminRecentBuyerResult::displayName));
        if (results.size() > command.limit()) {
            return List.copyOf(results.subList(0, command.limit()));
        }
        return List.copyOf(results);
    }

    private static final class BuyerAccumulator {
        private String displayName = "Cliente";
        private Instant lastOrderAt = Instant.EPOCH;
        private long orderCount;
        private com.superfercho.platform.money.Money lastOrderTotal =
                com.superfercho.platform.money.Money.cop(java.math.BigDecimal.ZERO);

        void accept(Order order) {
            orderCount++;
            if (order.createdAt().isAfter(lastOrderAt)) {
                lastOrderAt = order.createdAt();
                lastOrderTotal = order.total();
                String recipient = order.shippingAddress().recipientName();
                if (recipient != null && !recipient.isBlank()) {
                    displayName = recipient.trim();
                }
            }
        }
    }
}
