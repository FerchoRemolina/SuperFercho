package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AutoConfirmPendingOrdersUseCase {

    private final OrderRepository orderRepository;
    private final ClockProvider clockProvider;

    public AutoConfirmPendingOrdersUseCase(OrderRepository orderRepository, ClockProvider clockProvider) {
        this.orderRepository = orderRepository;
        this.clockProvider = clockProvider;
    }

    public List<OrderResult> execute() {
        Instant now = clockProvider.currentTime();
        List<OrderResult> confirmed = new ArrayList<>();
        for (Order order : orderRepository.findPendingOrdersEligibleForAutomaticConfirmation(now)) {
            if (order.isEligibleForAutomaticConfirmation(now)) {
                confirmed.add(OrderResult.from(orderRepository.save(order.confirm(now))));
            }
        }
        return List.copyOf(confirmed);
    }
}
