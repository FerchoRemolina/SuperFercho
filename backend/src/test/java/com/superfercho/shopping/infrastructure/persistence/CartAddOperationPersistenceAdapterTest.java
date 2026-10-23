package com.superfercho.shopping.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.shopping.application.dto.cart.CartAddOperation;
import com.superfercho.shopping.application.exception.DuplicateCartAddOperationException;
import com.superfercho.shopping.application.port.out.CartAddOperationPort;
import com.superfercho.shopping.infrastructure.persistence.repository.CartAddOperationJpaRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        properties = {
            "superfercho.scheduling.enabled=false",
            "superfercho.assistant.fercho.limits.max-requests-per-window=30",
            "superfercho.assistant.fercho.limits.window=PT24H",
            "superfercho.assistant.fercho.limits.pause=PT30M",
            "superfercho.assistant.fercho.limits.recidive-window=PT24H"
        })
@Testcontainers(disabledWithoutDocker = true)
class CartAddOperationPersistenceAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-23T18:00:00Z");

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
    private CartAddOperationPort cartAddOperationPort;

    @Autowired
    private CartAddOperationJpaRepository cartAddOperationJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldPersistAndReloadOperation() {
        UUID operationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CartAddOperation saved = new CartAddOperation(operationId, customerId, productId, 2, CREATED_AT);

        cartAddOperationPort.save(saved);

        CartAddOperation loaded = cartAddOperationPort.find(operationId).orElseThrow();
        assertThat(loaded.operationId()).isEqualTo(operationId);
        assertThat(loaded.customerId()).isEqualTo(customerId);
        assertThat(loaded.productId()).isEqualTo(productId);
        assertThat(loaded.quantity()).isEqualTo(2);
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(cartAddOperationJpaRepository.findByOperationId(operationId)).isPresent();
    }

    @Test
    void shouldRejectDuplicateOperationId() {
        UUID operationId = UUID.randomUUID();
        insertBypassingDomain(operationId, UUID.randomUUID(), UUID.randomUUID(), 1);

        assertThatThrownBy(() -> cartAddOperationPort.save(
                        new CartAddOperation(operationId, UUID.randomUUID(), UUID.randomUUID(), 3, CREATED_AT)))
                .isInstanceOf(DuplicateCartAddOperationException.class);
    }

    @Test
    void shouldRejectNonPositiveQuantityAtDatabase() {
        assertThatThrownBy(() -> insertBypassingDomain(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAndLockReturnsExistingRowInsideTransaction() {
        UUID operationId = UUID.randomUUID();
        CartAddOperation saved =
                new CartAddOperation(operationId, UUID.randomUUID(), UUID.randomUUID(), 2, CREATED_AT);
        cartAddOperationPort.save(saved);

        CartAddOperation locked = new TransactionTemplate(transactionManager).execute(
                status -> cartAddOperationPort.findAndLock(operationId).orElseThrow());

        assertThat(locked.operationId()).isEqualTo(operationId);
        assertThat(locked.quantity()).isEqualTo(2);
    }

    @Test
    void deleteAllByCustomerIdRemovesOnlyThatCustomersReceipts() {
        UUID customerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        UUID mine = UUID.randomUUID();
        UUID theirs = UUID.randomUUID();
        cartAddOperationPort.save(new CartAddOperation(mine, customerId, UUID.randomUUID(), 2, CREATED_AT));
        cartAddOperationPort.save(new CartAddOperation(theirs, otherCustomerId, UUID.randomUUID(), 1, CREATED_AT));

        cartAddOperationPort.deleteAllByCustomerId(customerId);

        assertThat(cartAddOperationPort.find(mine)).isEmpty();
        assertThat(cartAddOperationPort.find(theirs)).isPresent();
    }

    private void insertBypassingDomain(UUID operationId, UUID customerId, UUID productId, int quantity) {
        jdbcTemplate.update(
                """
                insert into shopping.cart_add_operations (
                    id, operation_id, customer_id, product_id, quantity, created_at
                ) values (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                operationId,
                customerId,
                productId,
                quantity,
                Timestamp.from(CREATED_AT));
    }
}
