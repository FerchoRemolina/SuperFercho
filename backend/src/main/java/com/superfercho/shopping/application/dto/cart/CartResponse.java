package com.superfercho.shopping.application.dto.cart;

import com.superfercho.platform.money.Money;
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
        Instant updatedAt,
        Money currentSubtotal,
        boolean currentPricingComplete) {

    public static CartResponse of(
            UUID id,
            UUID customerId,
            CartStatus status,
            List<CartItemResponse> items,
            Instant createdAt,
            Instant updatedAt) {
        boolean empty = items.isEmpty();
        return new CartResponse(
                id, customerId, status, items, createdAt, updatedAt, empty ? Money.copZero() : null, empty);
    }

    public static CartResponse from(
            Cart cart, List<CartItemResponse> pricedItems, Money currentSubtotal, boolean currentPricingComplete) {
        return new CartResponse(
                cart.id(),
                cart.customerId(),
                cart.status(),
                List.copyOf(pricedItems),
                cart.createdAt(),
                cart.updatedAt(),
                currentSubtotal,
                currentPricingComplete);
    }
}
