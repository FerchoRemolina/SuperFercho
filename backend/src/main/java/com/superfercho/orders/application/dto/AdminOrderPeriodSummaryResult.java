package com.superfercho.orders.application.dto;

import com.superfercho.platform.money.Money;

/**
 * Order counters for an arbitrary [from, to) period. In process = CONFIRMED +
 * PREPARING + DELIVERY. Sales exclude CANCELLED orders.
 */
public record AdminOrderPeriodSummaryResult(
        Money sales,
        long totalOrders,
        long inProcessOrders,
        long deliveredOrders,
        long cancelledOrders) {}
