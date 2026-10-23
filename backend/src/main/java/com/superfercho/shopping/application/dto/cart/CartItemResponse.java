package com.superfercho.shopping.application.dto.cart;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.domain.model.CartItem;
import java.time.Instant;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        int quantity,
        Money priceAtAddition,
        Money currentUnitPrice,
        Money currentLineAmount,
        Instant addedAt,
        Instant updatedAt) {

    public static CartItemResponse of(
            UUID id, UUID productId, int quantity, Money priceAtAddition, Instant addedAt, Instant updatedAt) {
        return new CartItemResponse(id, productId, quantity, priceAtAddition, null, null, addedAt, updatedAt);
    }

    public static CartItemResponse from(CartItem item) {
        return from(item, null);
    }

    public static CartItemResponse from(CartItem item, Money currentUnitPrice) {
        Money currentLineAmount = currentUnitPrice == null ? null : currentUnitPrice.times(item.quantity());
        return new CartItemResponse(
                item.id(),
                item.productId(),
                item.quantity(),
                item.priceAtAddition(),
                currentUnitPrice,
                currentLineAmount,
                item.addedAt(),
                item.updatedAt());
    }
}
