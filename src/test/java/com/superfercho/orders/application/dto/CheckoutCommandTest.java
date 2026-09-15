package com.superfercho.orders.application.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.orders.application.exception.InvalidCheckoutException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckoutCommandTest {

    private static final UUID ADDRESS_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));

    @Test
    void shouldRejectNullProductId() {
        assertThrows(InvalidCheckoutException.class, () -> new CheckoutItem(null, 1, PRICE));
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        assertThrows(InvalidCheckoutException.class, () -> new CheckoutItem(PRODUCT_ID, 0, PRICE));
        assertThrows(InvalidCheckoutException.class, () -> new CheckoutItem(PRODUCT_ID, -1, PRICE));
    }

    @Test
    void shouldRejectNullExpectedUnitPrice() {
        assertThrows(InvalidCheckoutException.class, () -> new CheckoutItem(PRODUCT_ID, 1, null));
    }

    @Test
    void shouldRejectDuplicateProductId() {
        CheckoutItem item = new CheckoutItem(PRODUCT_ID, 1, PRICE);

        assertThrows(
                InvalidCheckoutException.class,
                () -> new CheckoutCommand(
                        ADDRESS_ID, PaymentMethod.SIMULATED_CARD, List.of(item, new CheckoutItem(PRODUCT_ID, 2, PRICE)), "key-1"));
    }

    @Test
    void shouldCopyItemsImmutably() {
        CheckoutItem item = new CheckoutItem(PRODUCT_ID, 2, PRICE);
        CheckoutCommand command =
                new CheckoutCommand(ADDRESS_ID, PaymentMethod.SIMULATED_CARD, List.of(item), "key-1");

        assertEquals(1, command.items().size());
        assertThrows(UnsupportedOperationException.class, () -> command.items().add(item));
    }
}
