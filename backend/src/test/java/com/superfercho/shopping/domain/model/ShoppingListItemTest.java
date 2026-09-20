package com.superfercho.shopping.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.shopping.domain.exception.InvalidShoppingListItemException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ShoppingListItemTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant CREATED_AT = Instant.parse("2026-04-01T10:00:00Z");

    @Test
    void shouldCreateValidShoppingListItem() {
        ShoppingListItem item = ShoppingListItem.create(ID, PRODUCT_ID, 3, CREATED_AT);

        assertEquals(ID, item.id());
        assertEquals(PRODUCT_ID, item.productId());
        assertEquals(3, item.quantity());
        assertEquals(CREATED_AT, item.createdAt());
    }

    @Test
    void shouldRejectItemWhenIdIsNull() {
        assertThrows(
                InvalidShoppingListItemException.class, () -> ShoppingListItem.create(null, PRODUCT_ID, 1, CREATED_AT));
    }

    @Test
    void shouldRejectItemWhenProductIdIsNull() {
        assertThrows(
                InvalidShoppingListItemException.class, () -> ShoppingListItem.create(ID, null, 1, CREATED_AT));
    }

    @Test
    void shouldRejectItemWhenCreatedAtIsNull() {
        assertThrows(
                InvalidShoppingListItemException.class, () -> ShoppingListItem.create(ID, PRODUCT_ID, 1, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectItemWhenQuantityIsNotPositive(int quantity) {
        assertThrows(
                InvalidShoppingListItemException.class,
                () -> ShoppingListItem.create(ID, PRODUCT_ID, quantity, CREATED_AT));
    }

    @Test
    void shouldChangeQuantityAndPreserveIdentity() {
        ShoppingListItem original = ShoppingListItem.create(ID, PRODUCT_ID, 2, CREATED_AT);

        ShoppingListItem updated = original.changeQuantity(6);

        assertEquals(ID, updated.id());
        assertEquals(PRODUCT_ID, updated.productId());
        assertEquals(CREATED_AT, updated.createdAt());
        assertEquals(6, updated.quantity());
        assertEquals(2, original.quantity());
    }
}
