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
        Instant addedAt,
        Instant updatedAt) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.id(),
                item.productId(),
                item.quantity(),
                item.priceAtAddition(),
                item.addedAt(),
                item.updatedAt());
    }
}
