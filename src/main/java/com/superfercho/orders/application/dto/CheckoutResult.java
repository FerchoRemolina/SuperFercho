package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import java.util.UUID;

public record CheckoutResult(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        PaymentStatus paymentStatus,
        Money total) {
}
