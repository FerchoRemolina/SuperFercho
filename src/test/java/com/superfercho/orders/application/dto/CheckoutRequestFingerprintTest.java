package com.superfercho.orders.application.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckoutRequestFingerprintTest {

    private static final UUID ADDRESS_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_ADDRESS_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID PRODUCT_A = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PRODUCT_B = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));
    private static final Money OTHER_PRICE = Money.cop(new BigDecimal("12.00"));

    @Test
    void shouldProduceSameFingerprintForSameCommand() {
        assertEquals(CheckoutRequestFingerprint.from(baseCommand()), CheckoutRequestFingerprint.from(baseCommand()));
    }

    @Test
    void shouldProduceDifferentFingerprintForDifferentProduct() {
        CheckoutCommand otherProduct = command(
                ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_B, 2, PRICE)),
                "key-1");

        assertNotEquals(CheckoutRequestFingerprint.from(baseCommand()), CheckoutRequestFingerprint.from(otherProduct));
    }

    @Test
    void shouldProduceDifferentFingerprintForDifferentQuantity() {
        CheckoutCommand otherQuantity = command(
                ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_A, 3, PRICE)),
                "key-1");

        assertNotEquals(CheckoutRequestFingerprint.from(baseCommand()), CheckoutRequestFingerprint.from(otherQuantity));
    }

    @Test
    void shouldProduceDifferentFingerprintForDifferentPrice() {
        CheckoutCommand otherPrice = command(
                ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_A, 2, OTHER_PRICE)),
                "key-1");

        assertNotEquals(CheckoutRequestFingerprint.from(baseCommand()), CheckoutRequestFingerprint.from(otherPrice));
    }

    @Test
    void shouldProduceDifferentFingerprintForDifferentPaymentMethod() {
        CheckoutCommand otherMethod = command(
                ADDRESS_ID,
                PaymentMethod.CASH_ON_DELIVERY,
                List.of(new CheckoutItem(PRODUCT_A, 2, PRICE)),
                "key-1");

        assertNotEquals(CheckoutRequestFingerprint.from(baseCommand()), CheckoutRequestFingerprint.from(otherMethod));
    }

    @Test
    void shouldProduceDifferentFingerprintForDifferentAddress() {
        CheckoutCommand otherAddress = command(
                OTHER_ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_A, 2, PRICE)),
                "key-1");

        assertNotEquals(CheckoutRequestFingerprint.from(baseCommand()), CheckoutRequestFingerprint.from(otherAddress));
    }

    @Test
    void shouldProduceSameFingerprintWhenLineOrderDiffers() {
        CheckoutCommand original = command(
                ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_A, 2, PRICE), new CheckoutItem(PRODUCT_B, 1, OTHER_PRICE)),
                "key-1");
        CheckoutCommand reversed = command(
                ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_B, 1, OTHER_PRICE), new CheckoutItem(PRODUCT_A, 2, PRICE)),
                "key-1");

        assertEquals(CheckoutRequestFingerprint.from(original), CheckoutRequestFingerprint.from(reversed));
    }

    private static CheckoutCommand baseCommand() {
        return command(
                ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_A, 2, PRICE)),
                "key-1");
    }

    private static CheckoutCommand command(
            UUID addressId, PaymentMethod paymentMethod, List<CheckoutItem> items, String idempotencyKey) {
        return new CheckoutCommand(addressId, paymentMethod, items, idempotencyKey);
    }
}
