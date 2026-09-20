package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.UpdateOrderStatusCommand;
import com.superfercho.orders.application.exception.InvalidOrderStatusUpdateException;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.exception.InvalidOrderStateTransitionException;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import java.time.Instant;

public final class UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final ClockProvider clockProvider;

    public UpdateOrderStatusUseCase(OrderRepository orderRepository, ClockProvider clockProvider) {
        this.orderRepository = orderRepository;
        this.clockProvider = clockProvider;
    }

    public OrderResult execute(UpdateOrderStatusCommand command) {
        Order order = orderRepository
                .findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));
        Instant now = clockProvider.currentTime();
        Order updated = apply(order, command.status(), now);
        if (updated.status() == OrderStatus.CONFIRMED) {
            return OrderResult.from(orderRepository
                    .saveIfPending(updated)
                    .orElseThrow(
                            () -> new InvalidOrderStateTransitionException(OrderStatus.PENDING, OrderStatus.CONFIRMED)));
        }
        return OrderResult.from(orderRepository.save(updated));
    }

    private static Order apply(Order order, OrderStatus status, Instant now) {
        if (status == null) {
            throw new InvalidOrderStatusUpdateException(null);
        }
        return switch (status) {
            case CONFIRMED -> order.confirm(now);
            case PREPARING -> order.startPreparation(now);
            case READY -> order.markReady(now);
            case DELIVERED -> order.markDelivered(now);
            case PENDING, CANCELLED -> throw new InvalidOrderStatusUpdateException(status);
        };
    }
}
