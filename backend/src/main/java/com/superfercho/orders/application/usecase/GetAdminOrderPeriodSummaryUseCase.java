package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.AdminOrderPeriodSummaryResult;
import com.superfercho.orders.application.dto.AdminOrderStatusCountRow;
import com.superfercho.orders.application.dto.GetAdminDashboardSummaryCommand;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Order counters for an arbitrary [from, to) period, aggregated in the database.
 * In process = CONFIRMED + PREPARING + DELIVERY. Sales exclude CANCELLED orders;
 * cancelled orders keep their own counter.
 */
public final class GetAdminOrderPeriodSummaryUseCase {

    private final OrderRepository orderRepository;

    public GetAdminOrderPeriodSummaryUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public AdminOrderPeriodSummaryResult execute(GetAdminDashboardSummaryCommand command) {
        Objects.requireNonNull(command, "command");
        List<AdminOrderStatusCountRow> rows =
                orderRepository.countByStatusBetween(command.from(), command.to());

        BigDecimal sales = BigDecimal.ZERO.setScale(2);
        long totalOrders = 0;
        long inProcessOrders = 0;
        long deliveredOrders = 0;
        long cancelledOrders = 0;
        for (AdminOrderStatusCountRow row : rows) {
            totalOrders += row.orderCount();
            switch (row.status()) {
                case CONFIRMED, PREPARING, DELIVERY -> inProcessOrders += row.orderCount();
                case DELIVERED -> deliveredOrders += row.orderCount();
                case CANCELLED -> cancelledOrders += row.orderCount();
            }
            if (row.status() != OrderStatus.CANCELLED) {
                sales = sales.add(row.totalAmount());
            }
        }
        return new AdminOrderPeriodSummaryResult(
                Money.cop(sales), totalOrders, inProcessOrders, deliveredOrders, cancelledOrders);
    }
}
