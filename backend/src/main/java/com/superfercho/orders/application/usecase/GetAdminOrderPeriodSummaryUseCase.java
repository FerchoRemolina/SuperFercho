package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.AdminBusinessPeriodRow;
import com.superfercho.orders.application.dto.AdminOrderPeriodSummaryResult;
import com.superfercho.orders.application.dto.GetAdminDashboardSummaryCommand;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.platform.money.Money;
import java.util.Objects;

/**
 * Business summary for an arbitrary [from, to) period:
 *
 * <ul>
 *   <li>sales: DELIVERED orders only, recognized by {@code delivered_at}
 *       (PaymentStatus is not a criterion; CASH_ON_DELIVERY + PENDING counts
 *       once delivered).
 *   <li>delivered: DELIVERED count by {@code delivered_at}.
 *   <li>cancelled: CANCELLED count by {@code cancelled_at}.
 *   <li>in process: CURRENT live orders (CONFIRMED + PREPARING + DELIVERY) —
 *       independent of the period.
 * </ul>
 *
 * totalOrders = delivered + cancelled (orders closed inside the period).
 */
public final class GetAdminOrderPeriodSummaryUseCase {

    private final OrderRepository orderRepository;

    public GetAdminOrderPeriodSummaryUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public AdminOrderPeriodSummaryResult execute(GetAdminDashboardSummaryCommand command) {
        Objects.requireNonNull(command, "command");
        AdminBusinessPeriodRow row =
                orderRepository.summarizeBusinessPeriod(command.from(), command.to());
        long closedOrders = row.deliveredOrders() + row.cancelledOrders();
        return new AdminOrderPeriodSummaryResult(
                Money.cop(row.salesAmount()),
                closedOrders,
                row.inProcessOrders(),
                row.deliveredOrders(),
                row.cancelledOrders());
    }
}
