package com.superfercho.payments.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.payments.domain.exception.InvalidPaymentException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final UUID PAYMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant CREATED_AT = Instant.parse("2026-05-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-05-01T10:05:00Z");
    private static final Money AMOUNT = Money.cop(new BigDecimal("21.00"));
    private static final String PROVIDER_REFERENCE = "sim-approved";

    @Test
    void shouldCreatePayment() {
        Payment payment = Payment.create(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.APPROVED,
                PROVIDER_REFERENCE,
                CREATED_AT);

        assertEquals(PAYMENT_ID, payment.id());
        assertEquals(ORDER_ID, payment.orderId());
        assertEquals(AMOUNT, payment.amount());
        assertEquals(PaymentMethod.SIMULATED_CARD, payment.paymentMethod());
        assertEquals(PaymentStatus.APPROVED, payment.status());
        assertEquals(PROVIDER_REFERENCE, payment.providerReference());
        assertEquals(CREATED_AT, payment.createdAt());
        assertEquals(CREATED_AT, payment.updatedAt());
        assertNull(payment.refundedAt());
    }

    @Test
    void shouldAllowNullProviderReferenceOnCreate() {
        Payment payment = Payment.create(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentStatus.PENDING,
                null,
                CREATED_AT);

        assertNull(payment.providerReference());
        assertEquals(PaymentStatus.PENDING, payment.status());
    }

    @Test
    void shouldRejectPaymentWhenIdIsNull() {
        assertThrows(
                InvalidPaymentException.class,
                () -> Payment.create(
                        null,
                        ORDER_ID,
                        AMOUNT,
                        PaymentMethod.SIMULATED_CARD,
                        PaymentStatus.APPROVED,
                        PROVIDER_REFERENCE,
                        CREATED_AT));
    }

    @Test
    void shouldRejectPaymentWhenOrderIdIsNull() {
        assertThrows(
                InvalidPaymentException.class,
                () -> Payment.create(
                        PAYMENT_ID,
                        null,
                        AMOUNT,
                        PaymentMethod.SIMULATED_CARD,
                        PaymentStatus.APPROVED,
                        PROVIDER_REFERENCE,
                        CREATED_AT));
    }

    @Test
    void shouldRejectPaymentWhenAmountIsNull() {
        assertThrows(
                InvalidPaymentException.class,
                () -> Payment.create(
                        PAYMENT_ID,
                        ORDER_ID,
                        null,
                        PaymentMethod.SIMULATED_CARD,
                        PaymentStatus.APPROVED,
                        PROVIDER_REFERENCE,
                        CREATED_AT));
    }

    @Test
    void shouldRejectPaymentWhenPaymentMethodIsNull() {
        assertThrows(
                InvalidPaymentException.class,
                () -> Payment.create(
                        PAYMENT_ID,
                        ORDER_ID,
                        AMOUNT,
                        null,
                        PaymentStatus.APPROVED,
                        PROVIDER_REFERENCE,
                        CREATED_AT));
    }

    @Test
    void shouldRejectPaymentWhenStatusIsNull() {
        assertThrows(
                InvalidPaymentException.class,
                () -> Payment.create(
                        PAYMENT_ID,
                        ORDER_ID,
                        AMOUNT,
                        PaymentMethod.SIMULATED_CARD,
                        null,
                        PROVIDER_REFERENCE,
                        CREATED_AT));
    }

    @Test
    void shouldRejectPaymentWhenCreatedAtIsNull() {
        assertThrows(
                InvalidPaymentException.class,
                () -> Payment.create(
                        PAYMENT_ID,
                        ORDER_ID,
                        AMOUNT,
                        PaymentMethod.SIMULATED_CARD,
                        PaymentStatus.APPROVED,
                        PROVIDER_REFERENCE,
                        null));
    }

    @Test
    void shouldReconstitutePaymentWithNullRefundedAt() {
        Payment payment = Payment.reconstitute(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentStatus.PENDING,
                "cod-pending",
                CREATED_AT,
                LATER,
                null);

        assertEquals(PAYMENT_ID, payment.id());
        assertEquals(ORDER_ID, payment.orderId());
        assertEquals(AMOUNT, payment.amount());
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, payment.paymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.status());
        assertEquals("cod-pending", payment.providerReference());
        assertEquals(CREATED_AT, payment.createdAt());
        assertEquals(LATER, payment.updatedAt());
        assertNull(payment.refundedAt());
    }

    @Test
    void shouldReconstitutePaymentWithRefundedAt() {
        Payment payment = Payment.reconstitute(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.APPROVED,
                PROVIDER_REFERENCE,
                CREATED_AT,
                LATER,
                LATER);

        assertEquals(PaymentStatus.APPROVED, payment.status());
        assertEquals(LATER, payment.refundedAt());
        assertEquals(LATER, payment.updatedAt());
    }

    @Test
    void shouldRefundApprovedPayment() {
        Payment refunded = approvedPayment().refund(LATER);

        assertEquals(LATER, refunded.refundedAt());
        assertEquals(LATER, refunded.updatedAt());
        assertEquals(PaymentStatus.APPROVED, refunded.status());
        assertEquals(PAYMENT_ID, refunded.id());
        assertEquals(ORDER_ID, refunded.orderId());
        assertEquals(AMOUNT, refunded.amount());
        assertEquals(PaymentMethod.SIMULATED_CARD, refunded.paymentMethod());
        assertEquals(PROVIDER_REFERENCE, refunded.providerReference());
        assertEquals(CREATED_AT, refunded.createdAt());
    }

    @Test
    void shouldRejectRefundWhenPending() {
        Payment payment = Payment.create(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentStatus.PENDING,
                "cod-pending",
                CREATED_AT);

        assertThrows(InvalidPaymentException.class, () -> payment.refund(LATER));
    }

    @Test
    void shouldRejectRefundWhenDeclined() {
        Payment payment = Payment.create(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.DECLINED,
                "sim-declined",
                CREATED_AT);

        assertThrows(InvalidPaymentException.class, () -> payment.refund(LATER));
    }

    @Test
    void shouldRejectRefundWhenAlreadyRefunded() {
        Payment refunded = approvedPayment().refund(LATER);

        assertThrows(InvalidPaymentException.class, () -> refunded.refund(LATER.plusSeconds(1)));
    }

    @Test
    void shouldRejectRefundWhenRefundedAtIsNull() {
        assertThrows(InvalidPaymentException.class, () -> approvedPayment().refund(null));
    }

    private static Payment approvedPayment() {
        return Payment.create(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.APPROVED,
                PROVIDER_REFERENCE,
                CREATED_AT);
    }
}
