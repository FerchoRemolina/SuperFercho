package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.ListAdminOrdersCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;

public final class ListAdminOrdersUseCase {

    private final OrderRepository orderRepository;

    public ListAdminOrdersUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public PagedResult<OrderResult> execute(ListAdminOrdersCommand command) {
        PageRequest pageRequest = PageRequest.of(command.page(), command.size());
        PagedResult<Order> orders = command.hasStatusFilter()
                ? orderRepository.findByStatuses(command.statuses(), pageRequest)
                : orderRepository.findAll(pageRequest);
        return new PagedResult<>(
                orders.items().stream().map(OrderResult::from).toList(),
                orders.page(),
                orders.size(),
                orders.totalElements());
    }
}
