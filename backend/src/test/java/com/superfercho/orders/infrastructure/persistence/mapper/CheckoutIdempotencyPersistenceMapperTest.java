package com.superfercho.orders.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.superfercho.orders.application.dto.CheckoutRequestFingerprint;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.IdempotencyRecord;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.infrastructure.persistence.entity.CheckoutIdempotencyJpaEntity;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckoutIdempotencyPersistenceMapperTest {

    private static final UUID ENTITY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ORDER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-09-16T10:00:00Z");
    private static final Money TOTAL = Money.cop(new BigDecimal("21.50"));
    private static final CheckoutRequestFingerprint FINGERPRINT =
            new CheckoutRequestFingerprint("addr|SIMULATED_CARD|prod:1:10.50:COP");

    private final CheckoutIdempotencyPersistenceMapper mapper = new CheckoutIdempotencyPersistenceMapper();

    @Test
    void shouldMapConfirmedApprovedRecordRoundTrip() {
        IdempotencyRecord record = record(OrderStatus.CONFIRMED, PaymentStatus.APPROVED, TOTAL);

        CheckoutIdempotencyJpaEntity entity = mapper.toEntity(ENTITY_ID, record);
        IdempotencyRecord mapped = mapper.toRecord(entity);

        assertEquals(ENTITY_ID, entity.getId());
        assertEquals(CUSTOMER_ID, entity.getCustomerId());
        assertEquals("checkout-key-1", entity.getIdempotencyKey());
        assertEquals(FINGERPRINT.value(), entity.getFingerprint());
        assertEquals(ORDER_ID, entity.getResultOrderId());
        assertEquals("ORD-P-1001", entity.getResultOrderNumber());
        assertEquals(OrderStatus.CONFIRMED, entity.getResultOrderStatus());
        assertEquals(PaymentStatus.APPROVED, entity.getResultPaymentStatus());
        assertEquals(new BigDecimal("21.50"), entity.getResultTotalAmount());
        assertEquals(Money.COP, entity.getResultTotalCurrency());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(EXPIRES_AT, entity.getExpiresAt());

        assertEquals(record.key(), mapped.key());
        assertEquals(record.customerId(), mapped.customerId());
        assertEquals(record.fingerprint(), mapped.fingerprint());
        assertEquals(record.result().orderId(), mapped.result().orderId());
        assertEquals(record.result().orderNumber(), mapped.result().orderNumber());
        assertEquals(record.result().status(), mapped.result().status());
        assertEquals(record.result().paymentStatus(), mapped.result().paymentStatus());
        assertEquals(record.result().total(), mapped.result().total());
        assertEquals(record.createdAt(), mapped.createdAt());
        assertEquals(record.expiresAt(), mapped.expiresAt());
    }

    @Test
    void shouldMapPendingCashOnDeliveryResult() {
        IdempotencyRecord record = record(OrderStatus.PENDING, PaymentStatus.PENDING, TOTAL);

        CheckoutIdempotencyJpaEntity entity = mapper.toEntity(ENTITY_ID, record);

        assertEquals(OrderStatus.PENDING, entity.getResultOrderStatus());
        assertEquals(PaymentStatus.PENDING, entity.getResultPaymentStatus());
        assertEquals(OrderStatus.PENDING, mapper.toRecord(entity).result().status());
        assertEquals(PaymentStatus.PENDING, mapper.toRecord(entity).result().paymentStatus());
    }

    @Test
    void shouldMapCancelledDeclinedResult() {
        IdempotencyRecord record = record(OrderStatus.CANCELLED, PaymentStatus.DECLINED, Money.cop(BigDecimal.ZERO));

        CheckoutIdempotencyJpaEntity entity = mapper.toEntity(ENTITY_ID, record);
        IdempotencyRecord mapped = mapper.toRecord(entity);

        assertEquals(OrderStatus.CANCELLED, entity.getResultOrderStatus());
        assertEquals(PaymentStatus.DECLINED, entity.getResultPaymentStatus());
        assertEquals(new BigDecimal("0.00"), entity.getResultTotalAmount());
        assertEquals(Money.COP, entity.getResultTotalCurrency());
        assertEquals(Money.cop(new BigDecimal("0.00")), mapped.result().total());
    }

    @Test
    void shouldPreserveMoneyScaleAndCurrencyWithoutStringConversion() {
        Money total = Money.cop(new BigDecimal("10.50"));
        CheckoutIdempotencyJpaEntity entity = mapper.toEntity(ENTITY_ID, record(OrderStatus.CONFIRMED, PaymentStatus.APPROVED, total));

        assertEquals(0, total.amount().compareTo(entity.getResultTotalAmount()));
        assertEquals(2, entity.getResultTotalAmount().scale());
        assertEquals(Money.COP, entity.getResultTotalCurrency());
        assertEquals(total, mapper.toRecord(entity).result().total());
    }

    private static IdempotencyRecord record(OrderStatus orderStatus, PaymentStatus paymentStatus, Money total) {
        return new IdempotencyRecord(
                "checkout-key-1",
                CUSTOMER_ID,
                FINGERPRINT,
                new CheckoutResult(ORDER_ID, "ORD-P-1001", orderStatus, paymentStatus, total),
                CREATED_AT,
                EXPIRES_AT);
    }
}
