package com.superfercho.shopping.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.infrastructure.security.AuthenticatedUserPrincipal;
import com.superfercho.shopping.application.dto.cart.IdempotentAddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.IdempotentCartAddResult;
import com.superfercho.shopping.application.exception.CartAddOperationConflictException;
import com.superfercho.shopping.application.port.in.ApplyIdempotentCartAddUseCase;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.domain.model.Cart;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
class TransactionalIdempotentCartAddIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T18:00:00Z");

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
    private ApplyIdempotentCartAddUseCase applyIdempotentCartAddUseCase;

    @Autowired
    private CartRepositoryPort cartRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void firstApplyWritesCartAndReceiptTogether() {
        UUID customerId = UUID.randomUUID();
        UUID productId = insertCatalogProduct();
        UUID operationId = UUID.randomUUID();
        authenticate(customerId);

        IdempotentCartAddResult result = applyIdempotentCartAddUseCase.execute(
                new IdempotentAddProductToCartCommand(operationId, productId, 2));

        assertThat(result.replayed()).isFalse();
        assertThat(cartQuantity(customerId, productId)).isEqualTo(2);
        assertThat(countReceipts(operationId)).isEqualTo(1);
    }

    @Test
    void replayWithSameFingerprintDoesNotIncrementQuantity() {
        UUID customerId = UUID.randomUUID();
        UUID productId = insertCatalogProduct();
        UUID operationId = UUID.randomUUID();
        authenticate(customerId);
        applyIdempotentCartAddUseCase.execute(new IdempotentAddProductToCartCommand(operationId, productId, 2));

        IdempotentCartAddResult replay = applyIdempotentCartAddUseCase.execute(
                new IdempotentAddProductToCartCommand(operationId, productId, 2));

        assertThat(replay.replayed()).isTrue();
        assertThat(cartQuantity(customerId, productId)).isEqualTo(2);
        assertThat(countReceipts(operationId)).isEqualTo(1);
        assertThat(applyIdempotentCartAddUseCase.replayIfApplied(operationId, 2))
                .isPresent()
                .get()
                .extracting(IdempotentCartAddResult::replayed)
                .isEqualTo(true);
    }

    @Test
    void sameKeyDifferentQuantityConflicts() {
        UUID customerId = UUID.randomUUID();
        UUID productId = insertCatalogProduct();
        UUID operationId = UUID.randomUUID();
        authenticate(customerId);
        applyIdempotentCartAddUseCase.execute(new IdempotentAddProductToCartCommand(operationId, productId, 2));

        assertThatThrownBy(() -> applyIdempotentCartAddUseCase.execute(
                        new IdempotentAddProductToCartCommand(operationId, productId, 3)))
                .isInstanceOf(CartAddOperationConflictException.class);
        assertThat(cartQuantity(customerId, productId)).isEqualTo(2);
    }

    @Test
    void concurrentSameOperationIdAddsOnce() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID productId = insertCatalogProduct();
        UUID operationId = UUID.randomUUID();
        IdempotentAddProductToCartCommand command =
                new IdempotentAddProductToCartCommand(operationId, productId, 2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<IdempotentCartAddResult> first = pool.submit(() -> executeAuthenticated(customerId, command, ready, start));
            Future<IdempotentCartAddResult> second =
                    pool.submit(() -> executeAuthenticated(customerId, command, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            IdempotentCartAddResult a = first.get(30, TimeUnit.SECONDS);
            IdempotentCartAddResult b = second.get(30, TimeUnit.SECONDS);

            assertThat(a.cart().id()).isEqualTo(b.cart().id());
            authenticate(customerId);
            assertThat(cartQuantity(customerId, productId)).isEqualTo(2);
            assertThat(countReceipts(operationId)).isEqualTo(1);
            assertThat(a.replayed() && b.replayed()).isFalse();
            assertThat(a.replayed() || b.replayed()).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rollbackLeavesNeitherReceiptNorCartIncrement() {
        UUID customerId = UUID.randomUUID();
        UUID productId = insertCatalogProduct();
        UUID operationId = UUID.randomUUID();
        authenticate(customerId);
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        template.executeWithoutResult(status -> {
            applyIdempotentCartAddUseCase.execute(new IdempotentAddProductToCartCommand(operationId, productId, 2));
            status.setRollbackOnly();
        });

        assertThat(cartRepository.findByCustomerId(customerId)).isEmpty();
        assertThat(countReceipts(operationId)).isZero();
    }

    private IdempotentCartAddResult executeAuthenticated(
            UUID customerId,
            IdempotentAddProductToCartCommand command,
            CountDownLatch ready,
            CountDownLatch start)
            throws InterruptedException {
        authenticate(customerId);
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("timed out waiting to start idempotent add");
        }
        return applyIdempotentCartAddUseCase.execute(command);
    }

    private void authenticate(UUID customerId) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(customerId, Role.CUSTOMER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private int cartQuantity(UUID customerId, UUID productId) {
        Cart cart = cartRepository.findByCustomerId(customerId).orElseThrow();
        return cart.items().stream()
                .filter(item -> item.productId().equals(productId))
                .findFirst()
                .orElseThrow()
                .quantity();
    }

    private int countReceipts(UUID operationId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from shopping.cart_add_operations where operation_id = ?",
                Integer.class,
                operationId);
        return count == null ? 0 : count;
    }

    private UUID insertCatalogProduct() {
        UUID categoryId = UUID.randomUUID();
        UUID productTypeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Timestamp now = Timestamp.from(NOW);
        jdbcTemplate.update(
                """
                insert into catalog.categories (id, name, description, status, created_at, updated_at)
                values (?, ?, null, 'ACTIVE', ?, ?)
                """,
                categoryId,
                "Cat-" + categoryId.toString().substring(0, 8),
                now,
                now);
        jdbcTemplate.update(
                """
                insert into catalog.product_types (
                    id, category_id, name, description, status, created_at, updated_at
                ) values (?, ?, ?, null, 'ACTIVE', ?, ?)
                """,
                productTypeId,
                categoryId,
                "Type-" + productTypeId.toString().substring(0, 8),
                now,
                now);
        jdbcTemplate.update(
                """
                insert into catalog.products (
                    id, category_id, product_type_id, presentation_quantity, presentation_unit,
                    name, price_amount, currency, stock, status, created_at, updated_at
                ) values (?, ?, ?, 1, 'UNIT', ?, 1000.00, 'COP', 20, 'ACTIVE', ?, ?)
                """,
                productId,
                categoryId,
                productTypeId,
                "Leche-" + productId.toString().substring(0, 8),
                now,
                now);
        return productId;
    }
}
