package com.superfercho.orders.application.dto;

import com.superfercho.platform.money.Money;
import java.util.UUID;

public record PaymentRequest(UUID orderId, Money amount, PaymentMethod paymentMethod) {
}
