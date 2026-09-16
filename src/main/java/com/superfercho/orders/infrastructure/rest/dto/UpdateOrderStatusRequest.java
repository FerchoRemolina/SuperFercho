package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.domain.model.OrderStatus;

public record UpdateOrderStatusRequest(OrderStatus status) {
}
