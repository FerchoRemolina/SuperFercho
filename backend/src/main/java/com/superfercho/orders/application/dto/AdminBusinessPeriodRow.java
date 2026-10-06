package com.superfercho.orders.application.dto;

import java.math.BigDecimal;

/**
 * Business summary of a [from, to) period:
 *
 * <ul>
 *   <li>deliveredOrders: orders with status DELIVERED and delivered_at in the period.
 *   <li>cancelledOrders: orders with status CANCELLED and cancelled_at in the period.
 *   <li>salesAmount: SUM(total) of DELIVERED orders by delivered_at (a sale is
 *       recognized on delivery; PaymentStatus is not a criterion).
 *   <li>inProcessOrders: CURRENT live orders (CONFIRMED + PREPARING + DELIVERY),
 *       independent of the period.
 * </ul>
 */
public record AdminBusinessPeriodRow(
        long deliveredOrders,
        long cancelledOrders,
        BigDecimal salesAmount,
        long inProcessOrders) {}
