package com.superfercho.shopping.infrastructure.rest.dto;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.cart.CartItemResponse;
import java.time.Instant;
import java.util.UUID;

public record CartItemRestResponse(
        UUID id,
        UUID productId,
        int quantity,
        Money priceAtAddition,
        Instant addedAt,
        Instant updatedAt) {

    public static CartItemRestResponse from(CartItemResponse item) {
        return new CartItemRestResponse(
                item.id(),
                item.productId(),
                item.quantity(),
                item.priceAtAddition(),
                item.addedAt(),
                item.updatedAt());
    }
}
