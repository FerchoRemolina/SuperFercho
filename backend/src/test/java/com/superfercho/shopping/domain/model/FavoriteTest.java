package com.superfercho.shopping.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.shopping.domain.exception.InvalidFavoriteException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FavoriteTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant CREATED_AT = Instant.parse("2026-09-21T10:00:00Z");

    @Test
    void shouldCreateValidFavorite() {
        Favorite favorite = Favorite.create(ID, CUSTOMER_ID, PRODUCT_ID, CREATED_AT);

        assertEquals(ID, favorite.id());
        assertEquals(CUSTOMER_ID, favorite.customerId());
        assertEquals(PRODUCT_ID, favorite.productId());
        assertEquals(CREATED_AT, favorite.createdAt());
    }

    @Test
    void shouldRejectFavoriteWhenIdIsNull() {
        assertThrows(
                InvalidFavoriteException.class, () -> Favorite.create(null, CUSTOMER_ID, PRODUCT_ID, CREATED_AT));
    }

    @Test
    void shouldRejectFavoriteWhenCustomerIdIsNull() {
        assertThrows(InvalidFavoriteException.class, () -> Favorite.create(ID, null, PRODUCT_ID, CREATED_AT));
    }

    @Test
    void shouldRejectFavoriteWhenProductIdIsNull() {
        assertThrows(InvalidFavoriteException.class, () -> Favorite.create(ID, CUSTOMER_ID, null, CREATED_AT));
    }

    @Test
    void shouldRejectFavoriteWhenCreatedAtIsNull() {
        assertThrows(InvalidFavoriteException.class, () -> Favorite.create(ID, CUSTOMER_ID, PRODUCT_ID, null));
    }
}
