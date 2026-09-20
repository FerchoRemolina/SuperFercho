package com.superfercho.orders.application.dto;

import java.util.UUID;

public record CancelOrderCommand(UUID orderId) {
}
