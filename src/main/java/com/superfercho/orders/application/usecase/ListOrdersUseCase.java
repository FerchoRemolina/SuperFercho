package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.ListOrdersCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.port.CurrentUserProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;

public final class ListOrdersUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final OrderRepository orderRepository;

    public ListOrdersUseCase(CurrentUserProvider currentUserProvider, OrderRepository orderRepository) {
        this.currentUserProvider = currentUserProvider;
        this.orderRepository = orderRepository;
    }

    public PagedResult<OrderResult> execute(ListOrdersCommand command) {
        PageRequest pageRequest = PageRequest.of(command.page(), command.size());
        PagedResult<Order> orders =
                orderRepository.findByCustomerId(currentUserProvider.getCurrentUserId(), pageRequest);
        return new PagedResult<>(
                orders.items().stream().map(OrderResult::from).toList(),
                orders.page(),
                orders.size(),
                orders.totalElements());
    }
}
