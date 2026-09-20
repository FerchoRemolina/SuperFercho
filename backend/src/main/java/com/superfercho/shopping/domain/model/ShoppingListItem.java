package com.superfercho.shopping.domain.model;

import com.superfercho.shopping.domain.exception.InvalidShoppingListItemException;
import java.time.Instant;
import java.util.UUID;

public record ShoppingListItem(UUID id, UUID productId, int quantity, Instant createdAt) {

    public ShoppingListItem {
        requireNonNull(id, "id");
        requireNonNull(productId, "productId");
        if (quantity <= 0) {
            throw new InvalidShoppingListItemException("quantity must be greater than 0");
        }
        requireNonNull(createdAt, "createdAt");
    }

    public static ShoppingListItem create(UUID id, UUID productId, int quantity, Instant createdAt) {
        return new ShoppingListItem(id, productId, quantity, createdAt);
    }

    public ShoppingListItem changeQuantity(int quantity) {
        return new ShoppingListItem(id, productId, quantity, createdAt);
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidShoppingListItemException(field + " cannot be null");
        }
    }
}
