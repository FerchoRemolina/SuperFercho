package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.model.OrderStatus;
import java.util.UUID;

public record UpdateOrderStatusCommand(UUID orderId, OrderStatus status) {
}
