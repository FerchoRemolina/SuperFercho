package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.AdminOrderPeriodSummaryResult;
import com.superfercho.platform.money.Money;
import java.time.Instant;

/**
 * Order counters for an arbitrary [from, to) period. Sales exclude CANCELLED
 * orders; cancelled orders keep their own counter.
 */
public record AdminDashboardSummaryRestResponse(
        Instant from,
        Instant to,
        Money sales,
        long totalOrders,
        long inProcessOrders,
        long deliveredOrders,
        long cancelledOrders) {

    public static AdminDashboardSummaryRestResponse from(
            AdminOrderPeriodSummaryResult result, Instant from, Instant to) {
        return new AdminDashboardSummaryRestResponse(
                from,
                to,
                result.sales(),
                result.totalOrders(),
                result.inProcessOrders(),
                result.deliveredOrders(),
                result.cancelledOrders());
    }
}
