package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.model.OrderStatus;
import java.math.BigDecimal;

/** Order count and sales amount for one status inside a [from, to) period. */
public record AdminOrderStatusCountRow(OrderStatus status, long orderCount, BigDecimal totalAmount) {}
