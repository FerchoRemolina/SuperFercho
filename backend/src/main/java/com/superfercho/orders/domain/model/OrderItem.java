package com.superfercho.orders.domain.model;

import com.superfercho.orders.domain.exception.InvalidOrderItemException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderItem(
        UUID id,
        UUID productId,
        String productName,
        Money unitPrice,
        int quantity,
        Money subtotal) {

    public OrderItem {
        requireNonNull(id, "id");
        requireNonNull(productId, "productId");
        requireText(productName, "productName");
        requireNonNull(unitPrice, "unitPrice");
        if (quantity <= 0) {
            throw new InvalidOrderItemException("quantity must be greater than 0");
        }
        Money expectedSubtotal = subtotalOf(unitPrice, quantity);
        requireNonNull(subtotal, "subtotal");
        if (!expectedSubtotal.equals(subtotal)) {
            throw new InvalidOrderItemException("subtotal must equal unitPrice * quantity");
        }
    }

    public static OrderItem create(
            UUID id, UUID productId, String productName, Money unitPrice, int quantity) {
        if (quantity <= 0) {
            throw new InvalidOrderItemException("quantity must be greater than 0");
        }
        return new OrderItem(id, productId, productName, unitPrice, quantity, subtotalOf(unitPrice, quantity));
    }

    private static Money subtotalOf(Money unitPrice, int quantity) {
        requireNonNull(unitPrice, "unitPrice");
        return new Money(unitPrice.amount().multiply(BigDecimal.valueOf(quantity)), unitPrice.currency());
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidOrderItemException(field + " cannot be null");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderItemException(field + " cannot be null or blank");
        }
    }
}
