package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.port.CurrentUserProvider;
import com.superfercho.orders.application.port.OrderRepository;

public final class GetOrderUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final OrderRepository orderRepository;

    public GetOrderUseCase(CurrentUserProvider currentUserProvider, OrderRepository orderRepository) {
        this.currentUserProvider = currentUserProvider;
        this.orderRepository = orderRepository;
    }

    public OrderResult execute(GetOrderCommand command) {
        return OrderResult.from(OwnedOrderAccess.requireOwnedOrder(
                orderRepository, currentUserProvider.getCurrentUserId(), command.orderId()));
    }
}
