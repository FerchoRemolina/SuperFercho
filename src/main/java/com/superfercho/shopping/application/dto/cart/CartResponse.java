package com.superfercho.shopping.application.dto.cart;

import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID customerId,
        CartStatus status,
        List<CartItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.id(),
                cart.customerId(),
                cart.status(),
                cart.items().stream().map(CartItemResponse::from).toList(),
                cart.createdAt(),
                cart.updatedAt());
    }
}
