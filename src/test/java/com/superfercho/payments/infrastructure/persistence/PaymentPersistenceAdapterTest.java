package com.superfercho.payments.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.payments.application.port.PaymentRepository;
import com.superfercho.payments.domain.model.Payment;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
import com.superfercho.payments.infrastructure.persistence.repository.PaymentJpaRepository;
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
class PaymentPersistenceAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-05-01T10:05:00Z");
    private static final Money AMOUNT = Money.cop(new BigDecimal("21.00"));

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
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndReloadPaymentById() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment saved = paymentRepository.save(approvedCard(paymentId, orderId, "sim-approved"));

        assertThat(paymentJpaRepository.findById(saved.id())).isPresent();
        Payment loaded = paymentRepository.findById(paymentId).orElseThrow();

        assertThat(loaded.id()).isEqualTo(paymentId);
        assertThat(loaded.orderId()).isEqualTo(orderId);
        assertThat(loaded.amount()).isEqualTo(AMOUNT);
        assertThat(loaded.paymentMethod()).isEqualTo(PaymentMethod.SIMULATED_CARD);
        assertThat(loaded.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(loaded.providerReference()).isEqualTo("sim-approved");
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.updatedAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.refundedAt()).isNull();
    }

    @Test
    void shouldPersistRefundedAt() {
        UUID paymentId = UUID.randomUUID();
        Payment saved = paymentRepository.save(
                approvedCard(paymentId, UUID.randomUUID(), "sim-approved").refund(UPDATED_AT));

        Payment loaded = paymentRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(loaded.refundedAt()).isEqualTo(UPDATED_AT);
        assertThat(loaded.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldReturnEmptyWhenPaymentDoesNotExist() {
        assertThat(paymentRepository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldRoundTripPendingCashOnDeliveryWithNullProviderReference() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment saved = paymentRepository.save(Payment.create(
                paymentId,
                orderId,
                AMOUNT,
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentStatus.PENDING,
                null,
                CREATED_AT));

        Payment loaded = paymentRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(paymentId);
        assertThat(loaded.orderId()).isEqualTo(orderId);
        assertThat(loaded.amount()).isEqualTo(AMOUNT);
        assertThat(loaded.paymentMethod()).isEqualTo(PaymentMethod.CASH_ON_DELIVERY);
        assertThat(loaded.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(loaded.providerReference()).isNull();
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.updatedAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.refundedAt()).isNull();
    }

    @Test
    void shouldRejectInvalidStatusAtDatabase() {
        assertThatThrownBy(() -> insertPaymentBypassingDomain("REFUNDED", "SIMULATED_CARD", "COP", new BigDecimal("21.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNegativeAmountAtDatabase() {
        assertThatThrownBy(
                        () -> insertPaymentBypassingDomain(
                                "APPROVED", "SIMULATED_CARD", "COP", new BigDecimal("-0.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNonCopCurrencyAtDatabase() {
        assertThatThrownBy(
                        () -> insertPaymentBypassingDomain(
                                "APPROVED", "SIMULATED_CARD", "USD", new BigDecimal("21.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Payment approvedCard(UUID paymentId, UUID orderId, String providerReference) {
        return Payment.create(
                paymentId,
                orderId,
                AMOUNT,
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.APPROVED,
                providerReference,
                CREATED_AT);
    }

    private void insertPaymentBypassingDomain(
            String status, String paymentMethod, String currency, BigDecimal amount) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into payments.payments (
                                id, order_id, amount, currency, payment_method, status,
                                provider_reference, created_at, updated_at
                            ) values (?, ?, ?, ?, ?, ?, 'bypass', ?, ?)
                            """);
                    statement.setObject(1, UUID.randomUUID());
                    statement.setObject(2, UUID.randomUUID());
                    statement.setBigDecimal(3, amount);
                    statement.setString(4, currency);
                    statement.setString(5, paymentMethod);
                    statement.setString(6, status);
                    statement.setTimestamp(7, Timestamp.from(CREATED_AT));
                    statement.setTimestamp(8, Timestamp.from(CREATED_AT));
                    return statement;
                });
    }
}
