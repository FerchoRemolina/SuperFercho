package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.AdminOrderFilter;
import com.superfercho.orders.application.dto.AdminOrderListItemResult;
import com.superfercho.orders.application.dto.CustomerDirectoryEntry;
import com.superfercho.orders.application.dto.ListAdminOrdersCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.port.CustomerDirectoryPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Admin orders list page. Filters are applied in SQL and the page's customer
 * ids are resolved in bulk through {@link CustomerDirectoryPort} (two batch
 * queries per page, never per-row).
 */
public final class ListAdminOrdersUseCase {

    private final OrderRepository orderRepository;
    private final CustomerDirectoryPort customerDirectoryPort;

    public ListAdminOrdersUseCase(OrderRepository orderRepository, CustomerDirectoryPort customerDirectoryPort) {
        this.orderRepository = orderRepository;
        this.customerDirectoryPort = customerDirectoryPort;
    }

    public PagedResult<AdminOrderListItemResult> execute(ListAdminOrdersCommand command) {
        Objects.requireNonNull(command, "command");
        PageRequest pageRequest = PageRequest.of(command.page(), command.size());
        PagedResult<Order> page = orderRepository.findByAdminFilter(filterOf(command), pageRequest);

        Map<UUID, CustomerDirectoryEntry> customers = page.items().isEmpty()
                ? Map.of()
                : customerDirectoryPort.findByUserIds(
                        page.items().stream().map(Order::customerId).distinct().toList());

        List<AdminOrderListItemResult> items = page.items().stream()
                .map(order -> new AdminOrderListItemResult(
                        OrderResult.from(order), customers.get(order.customerId())))
                .toList();
        return new PagedResult<>(items, page.page(), page.size(), page.totalElements());
    }

    private static AdminOrderFilter filterOf(ListAdminOrdersCommand command) {
        return new AdminOrderFilter(
                command.statuses(), command.orderNumber(), command.fromInclusive(), command.toExclusive());
    }
}
