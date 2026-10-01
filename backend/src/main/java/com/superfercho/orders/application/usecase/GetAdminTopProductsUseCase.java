package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.AdminProductSalesRow;
import com.superfercho.orders.application.dto.GetAdminTopProductsCommand;
import com.superfercho.orders.application.port.OrderRepository;
import java.util.List;
import java.util.Objects;

/**
 * Product ranking by units sold for an arbitrary [from, to) period, aggregated
 * in the database. CANCELLED orders do not contribute. Only products ordered in
 * the period can appear (products without sales are not part of the ranking).
 */
public final class GetAdminTopProductsUseCase {

    private final OrderRepository orderRepository;

    public GetAdminTopProductsUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<AdminProductSalesRow> execute(GetAdminTopProductsCommand command) {
        Objects.requireNonNull(command, "command");
        return List.copyOf(orderRepository.findTopProductsByQuantity(
                command.from(), command.to(), command.limit(), command.ascending()));
    }
}
