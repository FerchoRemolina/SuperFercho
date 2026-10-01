package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.AdminCustomerSalesRow;
import com.superfercho.orders.application.dto.GetAdminTopCustomersCommand;
import com.superfercho.orders.application.port.OrderRepository;
import java.util.List;
import java.util.Objects;

/**
 * Top customers by purchased value for an arbitrary [from, to) period,
 * aggregated in the database. CANCELLED orders do not contribute.
 */
public final class GetAdminTopCustomersUseCase {

    private final OrderRepository orderRepository;

    public GetAdminTopCustomersUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<AdminCustomerSalesRow> execute(GetAdminTopCustomersCommand command) {
        Objects.requireNonNull(command, "command");
        return List.copyOf(
                orderRepository.findTopCustomersByTotal(command.from(), command.to(), command.limit()));
    }
}
