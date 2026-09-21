package com.superfercho.shopping.domain.model;

import com.superfercho.shopping.domain.exception.InvalidFavoriteException;
import java.time.Instant;
import java.util.UUID;

public record Favorite(UUID id, UUID customerId, UUID productId, Instant createdAt) {

    public Favorite {
        requireNonNull(id, "id");
        requireNonNull(customerId, "customerId");
        requireNonNull(productId, "productId");
        requireNonNull(createdAt, "createdAt");
    }

    public static Favorite create(UUID id, UUID customerId, UUID productId, Instant createdAt) {
        return new Favorite(id, customerId, productId, createdAt);
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidFavoriteException(field + " cannot be null");
        }
    }
}
