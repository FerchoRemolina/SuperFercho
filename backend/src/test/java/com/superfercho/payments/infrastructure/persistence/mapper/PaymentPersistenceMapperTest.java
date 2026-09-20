package com.superfercho.payments.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.superfercho.payments.domain.model.Payment;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
import com.superfercho.payments.infrastructure.persistence.entity.PaymentJpaEntity;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentPersistenceMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-05-01T10:05:00Z");
    private static final UUID PAYMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Money AMOUNT = Money.cop(new BigDecimal("21.00"));

    private final PaymentPersistenceMapper mapper = new PaymentPersistenceMapper();

    @Test
    void shouldMapApprovedPaymentRoundTrip() {
        Payment payment = Payment.create(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.APPROVED,
                "sim-approved",
                CREATED_AT);

        PaymentJpaEntity entity = mapper.toEntity(payment);
        Payment mapped = mapper.toDomain(entity);

        assertEquals(PAYMENT_ID, entity.getId());
        assertEquals(ORDER_ID, entity.getOrderId());
        assertEquals(new BigDecimal("21.00"), entity.getAmount());
        assertEquals(Money.COP, entity.getCurrency());
        assertEquals(PaymentMethod.SIMULATED_CARD, entity.getPaymentMethod());
        assertEquals(PaymentStatus.APPROVED, entity.getStatus());
        assertEquals("sim-approved", entity.getProviderReference());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(CREATED_AT, entity.getUpdatedAt());
        assertNull(entity.getRefundedAt());

        assertEquals(payment.id(), mapped.id());
        assertEquals(payment.orderId(), mapped.orderId());
        assertEquals(payment.amount(), mapped.amount());
        assertEquals(payment.paymentMethod(), mapped.paymentMethod());
        assertEquals(payment.status(), mapped.status());
        assertEquals(payment.providerReference(), mapped.providerReference());
        assertEquals(payment.createdAt(), mapped.createdAt());
        assertEquals(payment.updatedAt(), mapped.updatedAt());
        assertNull(mapped.refundedAt());
    }

    @Test
    void shouldMapNullProviderReference() {
        Payment payment = Payment.create(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentStatus.PENDING,
                null,
                CREATED_AT);

        Payment mapped = mapper.toDomain(mapper.toEntity(payment));

        assertNull(mapped.providerReference());
        assertEquals(PaymentStatus.PENDING, mapped.status());
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, mapped.paymentMethod());
    }

    @Test
    void shouldMapRefundedAtWhenPresent() {
        Payment refunded = Payment.create(
                        PAYMENT_ID,
                        ORDER_ID,
                        AMOUNT,
                        PaymentMethod.SIMULATED_CARD,
                        PaymentStatus.APPROVED,
                        "sim-approved",
                        CREATED_AT)
                .refund(UPDATED_AT);

        Payment mapped = mapper.toDomain(mapper.toEntity(refunded));

        assertEquals(PaymentStatus.APPROVED, mapped.status());
        assertEquals(UPDATED_AT, mapped.refundedAt());
        assertEquals(UPDATED_AT, mapped.updatedAt());
        assertEquals(CREATED_AT, mapped.createdAt());
    }

    @Test
    void shouldMapDeclinedPayment() {
        Payment payment = Payment.create(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.DECLINED,
                "sim-declined",
                CREATED_AT);

        Payment mapped = mapper.toDomain(mapper.toEntity(payment));

        assertEquals(PaymentStatus.DECLINED, mapped.status());
        assertNull(mapped.refundedAt());
    }
}
