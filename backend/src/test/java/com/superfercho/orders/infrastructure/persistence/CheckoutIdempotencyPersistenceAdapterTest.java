package com.superfercho.orders.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.orders.application.dto.CheckoutRequestFingerprint;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.IdempotencyRecord;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.infrastructure.persistence.repository.CheckoutIdempotencyJpaRepository;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CheckoutIdempotencyPersistenceAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-09-16T10:00:00Z");
    private static final Instant EXPIRED_AT = Instant.parse("2026-09-14T10:00:00Z");
    private static final Instant REUSE_AT = Instant.parse("2026-09-15T11:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Money TOTAL = Money.cop(new BigDecimal("21.50"));
    private static final String KEY = "checkout-key-shared";
    private static final CheckoutRequestFingerprint FINGERPRINT =
            new CheckoutRequestFingerprint("addr|SIMULATED_CARD|prod:1:10.50:COP");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                            DockerImageName.parse("pgvector/pgvector:pg16")
                                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("superfercho")
                    .withUsername("superfercho")
                    .withPassword("superfercho");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("superfercho.security.jwt.secret", () -> "test-only-superfercho-jwt-secret-key-32b");
    }

    @Autowired
    private IdempotencyPort idempotencyPort;

    @Autowired
    private CheckoutIdempotencyJpaRepository checkoutIdempotencyJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndReloadCheckoutIdempotency() {
        IdempotencyRecord record = record(CUSTOMER_ID, KEY, FINGERPRINT, CREATED_AT, EXPIRES_AT, TOTAL);

        idempotencyPort.save(record);
        IdempotencyRecord loaded = idempotencyPort.find(KEY, CUSTOMER_ID).orElseThrow();

        assertThat(loaded.key()).isEqualTo(KEY);
        assertThat(loaded.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(loaded.fingerprint()).isEqualTo(FINGERPRINT);
        assertThat(loaded.result().orderId()).isEqualTo(ORDER_ID);
        assertThat(loaded.result().orderNumber()).isEqualTo("ORD-P-1001");
        assertThat(loaded.result().status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(loaded.result().paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(loaded.result().total()).isEqualTo(TOTAL);
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(checkoutIdempotencyJpaRepository.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, KEY))
                .isPresent();
    }

    @Test
    void shouldReturnEmptyWhenKeyDoesNotExist() {
        assertThat(idempotencyPort.find("missing-key", CUSTOMER_ID)).isEmpty();
    }

    @Test
    void shouldReturnExpiredRecordFromFind() {
        String key = "expired-visible-key";
        idempotencyPort.save(record(CUSTOMER_ID, key, FINGERPRINT, CREATED_AT.minusSeconds(86_400), EXPIRED_AT, TOTAL));

        IdempotencyRecord loaded = idempotencyPort.find(key, CUSTOMER_ID).orElseThrow();

        assertThat(loaded.expiresAt()).isEqualTo(EXPIRED_AT);
        assertThat(loaded.expiresAt()).isBefore(CREATED_AT);
    }

    @Test
    void shouldAllowSameKeyForDifferentCustomers() {
        idempotencyPort.save(record(CUSTOMER_ID, KEY + "-multi", FINGERPRINT, CREATED_AT, EXPIRES_AT, TOTAL));
        idempotencyPort.save(record(
                OTHER_CUSTOMER_ID,
                KEY + "-multi",
                new CheckoutRequestFingerprint("other|CASH_ON_DELIVERY|prod:1:3.00:COP"),
                CREATED_AT,
                EXPIRES_AT,
                Money.cop(new BigDecimal("3.00"))));

        assertThat(idempotencyPort.find(KEY + "-multi", CUSTOMER_ID).orElseThrow().customerId())
                .isEqualTo(CUSTOMER_ID);
        assertThat(idempotencyPort.find(KEY + "-multi", OTHER_CUSTOMER_ID).orElseThrow().customerId())
                .isEqualTo(OTHER_CUSTOMER_ID);
        assertThat(checkoutIdempotencyJpaRepository.count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldRejectDuplicateCustomerAndKeyWhenNotExpired() {
        String key = "dup-active-key";
        idempotencyPort.save(record(CUSTOMER_ID, key, FINGERPRINT, CREATED_AT, EXPIRES_AT, TOTAL));

        assertThatThrownBy(() -> idempotencyPort.save(record(
                        CUSTOMER_ID,
                        key,
                        new CheckoutRequestFingerprint("other"),
                        CREATED_AT,
                        EXPIRES_AT,
                        TOTAL)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(checkoutIdempotencyJpaRepository.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, key))
                .hasValueSatisfying(entity -> assertThat(entity.getFingerprint()).isEqualTo(FINGERPRINT.value()));
    }

    @Test
    void shouldReuseExpiredRowWithoutViolatingUnique() {
        String key = "reuse-expired-key";
        idempotencyPort.save(record(
                CUSTOMER_ID, key, FINGERPRINT, CREATED_AT.minusSeconds(86_400), EXPIRED_AT, Money.cop(new BigDecimal("5.00"))));
        UUID originalId = checkoutIdempotencyJpaRepository
                .findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, key)
                .orElseThrow()
                .getId();

        CheckoutRequestFingerprint reusedFingerprint =
                new CheckoutRequestFingerprint("addr|CASH_ON_DELIVERY|prod:2:8.00:COP");
        Money reusedTotal = Money.cop(new BigDecimal("16.00"));
        Instant reusedExpiry = REUSE_AT.plusSeconds(86_400);
        idempotencyPort.save(new IdempotencyRecord(
                key,
                CUSTOMER_ID,
                reusedFingerprint,
                new CheckoutResult(ORDER_ID, "ORD-P-2002", OrderStatus.PENDING, PaymentStatus.PENDING, reusedTotal),
                REUSE_AT,
                reusedExpiry));

        assertThat(checkoutIdempotencyJpaRepository.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, key))
                .hasValueSatisfying(entity -> {
                    assertThat(entity.getId()).isEqualTo(originalId);
                    assertThat(entity.getFingerprint()).isEqualTo(reusedFingerprint.value());
                    assertThat(entity.getResultOrderNumber()).isEqualTo("ORD-P-2002");
                    assertThat(entity.getResultOrderStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(entity.getResultPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                    assertThat(entity.getResultTotalAmount()).isEqualByComparingTo("16.00");
                    assertThat(entity.getResultTotalCurrency()).isEqualTo(Money.COP);
                    assertThat(entity.getCreatedAt()).isEqualTo(REUSE_AT);
                    assertThat(entity.getExpiresAt()).isEqualTo(reusedExpiry);
                });
        assertThat(checkoutIdempotencyJpaRepository.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, key).stream()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void shouldAcquireAdvisoryLockWithoutError() {
        assertThat(idempotencyPort.find("lock-only-key", CUSTOMER_ID)).isEmpty();

        Long lockKey = jdbcTemplate.queryForObject(
                """
                select hashtextextended(
                    concat(cast(? as text), chr(31), cast(? as text)), 0)
                """,
                Long.class,
                CUSTOMER_ID.toString(),
                "lock-only-key");
        assertThat(lockKey).isNotNull();
    }

    @Test
    void shouldRejectInvalidOrderStatusAtDatabase() {
        assertThatThrownBy(() -> insertBypassingDomain("PAID", "APPROVED", "COP", new BigDecimal("21.50")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectInvalidPaymentStatusAtDatabase() {
        assertThatThrownBy(() -> insertBypassingDomain("CONFIRMED", "REFUNDED", "COP", new BigDecimal("21.50")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNegativeTotalAtDatabase() {
        assertThatThrownBy(() -> insertBypassingDomain("CONFIRMED", "APPROVED", "COP", new BigDecimal("-0.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNonCopCurrencyAtDatabase() {
        assertThatThrownBy(() -> insertBypassingDomain("CONFIRMED", "APPROVED", "USD", new BigDecimal("21.50")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateCustomerAndKeyAtDatabase() {
        UUID firstId = UUID.randomUUID();
        insertRow(firstId, CUSTOMER_ID, "sql-unique-key", "CONFIRMED", "APPROVED", "COP", new BigDecimal("21.50"));

        assertThatThrownBy(() -> insertRow(
                        UUID.randomUUID(),
                        CUSTOMER_ID,
                        "sql-unique-key",
                        "PENDING",
                        "PENDING",
                        "COP",
                        new BigDecimal("3.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static IdempotencyRecord record(
            UUID customerId,
            String key,
            CheckoutRequestFingerprint fingerprint,
            Instant createdAt,
            Instant expiresAt,
            Money total) {
        return new IdempotencyRecord(
                key,
                customerId,
                fingerprint,
                new CheckoutResult(ORDER_ID, "ORD-P-1001", OrderStatus.CONFIRMED, PaymentStatus.APPROVED, total),
                createdAt,
                expiresAt);
    }

    private void insertBypassingDomain(String orderStatus, String paymentStatus, String currency, BigDecimal amount) {
        insertRow(UUID.randomUUID(), CUSTOMER_ID, UUID.randomUUID().toString(), orderStatus, paymentStatus, currency, amount);
    }

    private void insertRow(
            UUID id,
            UUID customerId,
            String key,
            String orderStatus,
            String paymentStatus,
            String currency,
            BigDecimal amount) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into orders.checkout_idempotency (
                                id, customer_id, idempotency_key, fingerprint,
                                result_order_id, result_order_number, result_order_status,
                                result_payment_status, result_total_amount, result_total_currency,
                                created_at, expires_at
                            ) values (?, ?, ?, 'fp', ?, 'ORD-P-SQL', ?, ?, ?, ?, ?, ?)
                            """);
                    statement.setObject(1, id);
                    statement.setObject(2, customerId);
                    statement.setString(3, key);
                    statement.setObject(4, ORDER_ID);
                    statement.setString(5, orderStatus);
                    statement.setString(6, paymentStatus);
                    statement.setBigDecimal(7, amount);
                    statement.setString(8, currency);
                    statement.setTimestamp(9, Timestamp.from(CREATED_AT));
                    statement.setTimestamp(10, Timestamp.from(EXPIRES_AT));
                    return statement;
                });
    }
}
