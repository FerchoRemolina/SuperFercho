package com.superfercho.shopping.domain.model;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.domain.exception.InvalidCartItemException;
import java.time.Instant;
import java.util.UUID;

public record CartItem(
        UUID id,
        UUID productId,
        int quantity,
        Money priceAtAddition,
        Instant addedAt,
        Instant updatedAt) {

    public CartItem {
        requireNonNull(id, "id");
        requireNonNull(productId, "productId");
        requirePositiveQuantity(quantity);
        requireNonNull(priceAtAddition, "priceAtAddition");
        requireNonNull(addedAt, "addedAt");
        requireNonNull(updatedAt, "updatedAt");
        if (addedAt.isAfter(updatedAt)) {
            throw new InvalidCartItemException("addedAt must not be after updatedAt");
        }
    }

    public static CartItem create(
            UUID id,
            UUID productId,
            int quantity,
            Money priceAtAddition,
            Instant addedAt,
            Instant updatedAt) {
        return new CartItem(id, productId, quantity, priceAtAddition, addedAt, updatedAt);
    }

    public CartItem changeQuantity(int quantity, Instant updatedAt) {
        return new CartItem(id, productId, quantity, priceAtAddition, addedAt, updatedAt);
    }

    private static void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidCartItemException("quantity must be greater than 0");
        }
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidCartItemException(field + " cannot be null");
        }
    }
}
