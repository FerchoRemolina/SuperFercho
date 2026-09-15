package com.superfercho.orders.application.dto;

import com.superfercho.platform.money.Money;
import java.util.UUID;

public record OrderItemResult(
        UUID id, UUID productId, String productName, Money unitPrice, int quantity, Money subtotal) {
}
