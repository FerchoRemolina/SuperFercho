package com.superfercho.orders.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.CheckoutRequestFingerprint;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.IdempotencyRecord;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.infrastructure.persistence.entity.CheckoutIdempotencyJpaEntity;
import com.superfercho.orders.infrastructure.persistence.mapper.CheckoutIdempotencyPersistenceMapper;
import com.superfercho.orders.infrastructure.persistence.repository.CheckoutIdempotencyJpaRepository;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

@ExtendWith(MockitoExtension.class)
class IdempotencyPersistenceAdapterTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ORDER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID EXISTING_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final String KEY = "checkout-key-1";
    private static final Instant CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-09-16T10:00:00Z");
    private static final Instant EXPIRED_AT = Instant.parse("2026-09-14T10:00:00Z");
    private static final Instant REUSE_AT = Instant.parse("2026-09-15T11:00:00Z");
    private static final Money TOTAL = Money.cop(new BigDecimal("21.50"));
    private static final CheckoutRequestFingerprint FINGERPRINT =
            new CheckoutRequestFingerprint("addr|SIMULATED_CARD|prod:1:10.50:COP");

    @Mock
    private CheckoutIdempotencyJpaRepository repository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private final CheckoutIdempotencyPersistenceMapper mapper = new CheckoutIdempotencyPersistenceMapper();
    private IdempotencyPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdempotencyPersistenceAdapter(repository, mapper, jdbcTemplate);
    }

    @Test
    void shouldReturnEmptyWhenNoRowExistsAfterAdvisoryLock() {
        when(repository.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, KEY)).thenReturn(Optional.empty());

        Optional<IdempotencyRecord> found = adapter.find(KEY, CUSTOMER_ID);

        assertTrue(found.isEmpty());
        verifyAdvisoryLock();
        verify(repository).findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, KEY);
    }

    @Test
    void shouldReturnMappedRecordWhenFound() {
        when(repository.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, KEY))
                .thenReturn(Optional.of(entity(EXISTING_ID, EXPIRES_AT, CREATED_AT, TOTAL, FINGERPRINT.value())));

        IdempotencyRecord found = adapter.find(KEY, CUSTOMER_ID).orElseThrow();

        verifyAdvisoryLock();
        assertEquals(KEY, found.key());
        assertEquals(CUSTOMER_ID, found.customerId());
        assertEquals(FINGERPRINT, found.fingerprint());
        assertEquals(ORDER_ID, found.result().orderId());
        assertEquals("ORD-P-1001", found.result().orderNumber());
        assertEquals(OrderStatus.CONFIRMED, found.result().status());
        assertEquals(PaymentStatus.APPROVED, found.result().paymentStatus());
        assertEquals(TOTAL, found.result().total());
        assertEquals(CREATED_AT, found.createdAt());
        assertEquals(EXPIRES_AT, found.expiresAt());
    }

    @Test
    void shouldReturnExpiredRecordFromFind() {
        when(repository.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, KEY))
                .thenReturn(Optional.of(entity(EXISTING_ID, EXPIRED_AT, CREATED_AT.minusSeconds(86_400), TOTAL, FINGERPRINT.value())));

        IdempotencyRecord found = adapter.find(KEY, CUSTOMER_ID).orElseThrow();

        assertEquals(EXPIRED_AT, found.expiresAt());
        assertTrue(CREATED_AT.isAfter(found.expiresAt()));
        verifyAdvisoryLock();
    }

    @Test
    void shouldAssignUuidWhenSavingNewRecord() {
        when(repository.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, KEY)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        adapter.save(record(CREATED_AT, EXPIRES_AT, TOTAL, FINGERPRINT));

        CheckoutIdempotencyJpaEntity saved = capturedSave();
        assertEquals(CUSTOMER_ID, saved.getCustomerId());
        assertEquals(KEY, saved.getIdempotencyKey());
        assertEquals(FINGERPRINT.value(), saved.getFingerprint());
        assertEquals(ORDER_ID, saved.getResultOrderId());
        assertEquals("ORD-P-1001", saved.getResultOrderNumber());
        assertEquals(OrderStatus.CONFIRMED, saved.getResultOrderStatus());
        assertEquals(PaymentStatus.APPROVED, saved.getResultPaymentStatus());
        assertEquals(new BigDecimal("21.50"), saved.getResultTotalAmount());
        assertEquals(Money.COP, saved.getResultTotalCurrency());
        assertEquals(CREATED_AT, saved.getCreatedAt());
        assertEquals(EXPIRES_AT, saved.getExpiresAt());
        assertEquals(UUID.class, saved.getId().getClass());
    }

    @Test
    void shouldReuseExistingIdWhenSavingOverExpiredRow() {
        when(repository.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, KEY))
                .thenReturn(Optional.of(entity(
                        EXISTING_ID,
                        EXPIRED_AT,
                        CREATED_AT.minusSeconds(86_400),
                        Money.cop(new BigDecimal("5.00")),
                        "old-fingerprint")));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequestFingerprint newFingerprint = new CheckoutRequestFingerprint("new|CASH_ON_DELIVERY|prod:2:3.00:COP");
        Money newTotal = Money.cop(new BigDecimal("42.00"));
        Instant newExpiry = REUSE_AT.plusSeconds(86_400);
        adapter.save(new IdempotencyRecord(
                KEY,
                CUSTOMER_ID,
                newFingerprint,
                new CheckoutResult(ORDER_ID, "ORD-P-2002", OrderStatus.PENDING, PaymentStatus.PENDING, newTotal),
                REUSE_AT,
                newExpiry));

        CheckoutIdempotencyJpaEntity saved = capturedSave();
        assertEquals(EXISTING_ID, saved.getId());
        assertEquals(CUSTOMER_ID, saved.getCustomerId());
        assertEquals(KEY, saved.getIdempotencyKey());
        assertEquals(newFingerprint.value(), saved.getFingerprint());
        assertEquals(OrderStatus.PENDING, saved.getResultOrderStatus());
        assertEquals(PaymentStatus.PENDING, saved.getResultPaymentStatus());
        assertEquals(new BigDecimal("42.00"), saved.getResultTotalAmount());
        assertEquals(Money.COP, saved.getResultTotalCurrency());
        assertEquals(REUSE_AT, saved.getCreatedAt());
        assertEquals(newExpiry, saved.getExpiresAt());
    }

    @Test
    void shouldRejectOverwriteOfNonExpiredRow() {
        when(repository.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, KEY))
                .thenReturn(Optional.of(entity(EXISTING_ID, EXPIRES_AT, CREATED_AT, TOTAL, FINGERPRINT.value())));

        assertThrows(IllegalStateException.class, () -> adapter.save(record(CREATED_AT, EXPIRES_AT, TOTAL, FINGERPRINT)));
        verify(repository, never()).saveAndFlush(any());
    }

    @SuppressWarnings("unchecked")
    private void verifyAdvisoryLock() {
        verify(jdbcTemplate)
                .query(
                        eq(IdempotencyPersistenceAdapter.ADVISORY_LOCK_SQL),
                        any(ResultSetExtractor.class),
                        eq(CUSTOMER_ID.toString()),
                        eq(KEY));
    }

    private CheckoutIdempotencyJpaEntity capturedSave() {
        ArgumentCaptor<CheckoutIdempotencyJpaEntity> captor = ArgumentCaptor.forClass(CheckoutIdempotencyJpaEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static IdempotencyRecord record(
            Instant createdAt, Instant expiresAt, Money total, CheckoutRequestFingerprint fingerprint) {
        return new IdempotencyRecord(
                KEY,
                CUSTOMER_ID,
                fingerprint,
                new CheckoutResult(ORDER_ID, "ORD-P-1001", OrderStatus.CONFIRMED, PaymentStatus.APPROVED, total),
                createdAt,
                expiresAt);
    }

    private static CheckoutIdempotencyJpaEntity entity(
            UUID id, Instant expiresAt, Instant createdAt, Money total, String fingerprint) {
        return new CheckoutIdempotencyJpaEntity(
                id,
                CUSTOMER_ID,
                KEY,
                fingerprint,
                ORDER_ID,
                "ORD-P-1001",
                OrderStatus.CONFIRMED,
                PaymentStatus.APPROVED,
                total.amount(),
                total.currency(),
                createdAt,
                expiresAt);
    }
}
