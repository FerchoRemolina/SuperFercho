package com.superfercho.orders.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.orders.domain.exception.InvalidOrderItemException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderItemTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Money UNIT_PRICE = Money.cop(new BigDecimal("10.50"));

    @Test
    void shouldCreateValidOrderItemWithCalculatedSubtotal() {
        OrderItem item = OrderItem.create(ID, PRODUCT_ID, "Leche entera", UNIT_PRICE, 3);

        assertEquals(ID, item.id());
        assertEquals(PRODUCT_ID, item.productId());
        assertEquals("Leche entera", item.productName());
        assertEquals(UNIT_PRICE, item.unitPrice());
        assertEquals(3, item.quantity());
        assertEquals(Money.cop(new BigDecimal("31.50")), item.subtotal());
    }

    @Test
    void shouldRejectOrderItemWhenIdIsNull() {
        assertThrows(
                InvalidOrderItemException.class,
                () -> OrderItem.create(null, PRODUCT_ID, "Leche entera", UNIT_PRICE, 1));
    }

    @Test
    void shouldRejectOrderItemWhenProductIdIsNull() {
        assertThrows(
                InvalidOrderItemException.class, () -> OrderItem.create(ID, null, "Leche entera", UNIT_PRICE, 1));
    }

    @Test
    void shouldRejectOrderItemWhenProductNameIsBlank() {
        assertThrows(
                InvalidOrderItemException.class, () -> OrderItem.create(ID, PRODUCT_ID, "  ", UNIT_PRICE, 1));
    }

    @Test
    void shouldRejectOrderItemWhenUnitPriceIsNull() {
        assertThrows(
                InvalidOrderItemException.class, () -> OrderItem.create(ID, PRODUCT_ID, "Leche entera", null, 1));
    }

    @Test
    void shouldRejectOrderItemWhenUnitPriceIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OrderItem.create(
                        ID, PRODUCT_ID, "Leche entera", Money.cop(new BigDecimal("-0.01")), 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectOrderItemWhenQuantityIsNotPositive(int quantity) {
        assertThrows(
                InvalidOrderItemException.class,
                () -> OrderItem.create(ID, PRODUCT_ID, "Leche entera", UNIT_PRICE, quantity));
    }

    @Test
    void shouldRejectInconsistentSubtotal() {
        assertThrows(
                InvalidOrderItemException.class,
                () -> new OrderItem(
                        ID,
                        PRODUCT_ID,
                        "Leche entera",
                        UNIT_PRICE,
                        2,
                        Money.cop(new BigDecimal("10.50"))));
    }
}
