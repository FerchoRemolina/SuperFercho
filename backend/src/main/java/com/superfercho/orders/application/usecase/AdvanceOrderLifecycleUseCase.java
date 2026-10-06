package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Advances non-terminal orders CONFIRMED → PREPARING → DELIVERY → DELIVERED by {@code confirmedAt}
 * age. Catch-up jumps to the target status. Does not touch stock or payments. Applies to real
 * customers and storefront-preview temporary customers alike.
 */
public final class AdvanceOrderLifecycleUseCase {

    private final OrderRepository orderRepository;
    private final ClockProvider clockProvider;

    public AdvanceOrderLifecycleUseCase(OrderRepository orderRepository, ClockProvider clockProvider) {
        this.orderRepository = orderRepository;
        this.clockProvider = clockProvider;
    }

    public List<OrderResult> execute() {
        Instant now = clockProvider.currentTime();
        List<OrderResult> advanced = new ArrayList<>();
        for (Order order : orderRepository.findInProgressForLifecycle()) {
            if (order.status() == OrderStatus.DELIVERED || order.status() == OrderStatus.CANCELLED) {
                continue;
            }
            OrderStatus target = Order.targetStatusAt(order.confirmedAt(), now);
            if (target == order.status()) {
                continue;
            }
            Order next = order.advanceLifecycle(now);
            orderRepository
                    .saveIfCurrent(next, order.status())
                    .ifPresent(saved -> advanced.add(OrderResult.from(saved)));
        }
        return List.copyOf(advanced);
    }
}
