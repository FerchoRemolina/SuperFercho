package com.superfercho.shopping.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.domain.exception.InvalidCartItemException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CartItemTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));
    private static final Instant ADDED_AT = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-04-01T10:05:00Z");

    @Test
    void shouldCreateValidCartItem() {
        CartItem item = CartItem.create(ID, PRODUCT_ID, 2, PRICE, ADDED_AT, UPDATED_AT);

        assertEquals(ID, item.id());
        assertEquals(PRODUCT_ID, item.productId());
        assertEquals(2, item.quantity());
        assertEquals(PRICE, item.priceAtAddition());
        assertEquals(ADDED_AT, item.addedAt());
        assertEquals(UPDATED_AT, item.updatedAt());
    }

    @Test
    void shouldRejectCartItemWhenIdIsNull() {
        assertThrows(
                InvalidCartItemException.class,
                () -> CartItem.create(null, PRODUCT_ID, 1, PRICE, ADDED_AT, UPDATED_AT));
    }

    @Test
    void shouldRejectCartItemWhenProductIdIsNull() {
        assertThrows(
                InvalidCartItemException.class, () -> CartItem.create(ID, null, 1, PRICE, ADDED_AT, UPDATED_AT));
    }

    @Test
    void shouldRejectCartItemWhenPriceAtAdditionIsNull() {
        assertThrows(
                InvalidCartItemException.class,
                () -> CartItem.create(ID, PRODUCT_ID, 1, null, ADDED_AT, UPDATED_AT));
    }

    @Test
    void shouldRejectCartItemWhenAddedAtIsNull() {
        assertThrows(
                InvalidCartItemException.class, () -> CartItem.create(ID, PRODUCT_ID, 1, PRICE, null, UPDATED_AT));
    }

    @Test
    void shouldRejectCartItemWhenUpdatedAtIsNull() {
        assertThrows(
                InvalidCartItemException.class, () -> CartItem.create(ID, PRODUCT_ID, 1, PRICE, ADDED_AT, null));
    }

    @Test
    void shouldRejectCartItemWhenAddedAtIsAfterUpdatedAt() {
        assertThrows(
                InvalidCartItemException.class,
                () -> CartItem.create(ID, PRODUCT_ID, 1, PRICE, LATER, ADDED_AT));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectCartItemWhenQuantityIsNotPositive(int quantity) {
        assertThrows(
                InvalidCartItemException.class,
                () -> CartItem.create(ID, PRODUCT_ID, quantity, PRICE, ADDED_AT, UPDATED_AT));
    }

    @Test
    void shouldChangeQuantityAndPreserveIdentity() {
        CartItem original = CartItem.create(ID, PRODUCT_ID, 2, PRICE, ADDED_AT, UPDATED_AT);

        CartItem updated = original.changeQuantity(5, LATER);

        assertEquals(ID, updated.id());
        assertEquals(PRODUCT_ID, updated.productId());
        assertEquals(PRICE, updated.priceAtAddition());
        assertEquals(ADDED_AT, updated.addedAt());
        assertEquals(5, updated.quantity());
        assertEquals(LATER, updated.updatedAt());
        assertEquals(2, original.quantity());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectQuantityChangeWhenQuantityIsNotPositive(int quantity) {
        CartItem original = CartItem.create(ID, PRODUCT_ID, 2, PRICE, ADDED_AT, UPDATED_AT);

        assertThrows(InvalidCartItemException.class, () -> original.changeQuantity(quantity, LATER));
    }
}
