package com.superfercho.shopping.infrastructure.rest.dto;

import com.superfercho.shopping.application.dto.cart.CartResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartRestResponse(
        UUID id,
        UUID customerId,
        String status,
        List<CartItemRestResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    public static CartRestResponse from(CartResponse cart) {
        return new CartRestResponse(
                cart.id(),
                cart.customerId(),
                cart.status().name(),
                cart.items().stream().map(CartItemRestResponse::from).toList(),
                cart.createdAt(),
                cart.updatedAt());
    }
}
