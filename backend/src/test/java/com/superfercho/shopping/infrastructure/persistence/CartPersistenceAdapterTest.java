package com.superfercho.shopping.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartItem;
import com.superfercho.shopping.domain.model.CartStatus;
import com.superfercho.shopping.infrastructure.persistence.repository.CartJpaRepository;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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
class CartPersistenceAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:20:00Z");
    private static final Instant ADDED_AT = Instant.parse("2026-03-01T10:05:00Z");
    private static final Instant ITEM_UPDATED_AT = Instant.parse("2026-03-01T10:15:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SECOND_PRODUCT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

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
    private CartRepositoryPort cartRepository;

    @Autowired
    private CartJpaRepository cartJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndReloadCartById() {
        UUID customerId = UUID.randomUUID();
        Cart saved = cartRepository.save(cart(customerId, List.of(milk())));

        assertThat(cartJpaRepository.findById(saved.id())).isPresent();
        Cart loaded = cartRepository.findByCustomerId(customerId).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.customerId()).isEqualTo(customerId);
        assertThat(loaded.status()).isEqualTo(CartStatus.ACTIVE);
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(loaded.items()).hasSize(1);
        assertThat(loaded.items().get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(loaded.items().get(0).quantity()).isEqualTo(2);
        assertThat(loaded.items().get(0).priceAtAddition()).isEqualTo(Money.cop(new BigDecimal("10.50")));
        assertThat(loaded.items().get(0).addedAt()).isEqualTo(ADDED_AT);
        assertThat(loaded.items().get(0).updatedAt()).isEqualTo(ITEM_UPDATED_AT);
    }

    @Test
    void shouldPersistEmptyCart() {
        UUID customerId = UUID.randomUUID();
        Cart saved = cartRepository.save(cart(customerId, List.of()));

        Cart loaded = cartRepository.findByCustomerId(customerId).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.items()).isEmpty();
        assertThat(loaded.status()).isEqualTo(CartStatus.ACTIVE);
    }

    @Test
    void shouldPersistMultipleItemsAndPreserveOrder() {
        UUID customerId = UUID.randomUUID();
        cartRepository.save(cart(customerId, List.of(milk(), bread())));

        Cart loaded = cartRepository.findByCustomerId(customerId).orElseThrow();

        assertThat(loaded.items()).hasSize(2);
        assertThat(loaded.items().get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(loaded.items().get(1).productId()).isEqualTo(SECOND_PRODUCT_ID);
        assertThat(loaded.items().get(0).quantity()).isEqualTo(2);
        assertThat(loaded.items().get(1).quantity()).isEqualTo(1);
        assertThat(loaded.items().get(0).priceAtAddition()).isEqualTo(Money.cop(new BigDecimal("10.50")));
        assertThat(loaded.items().get(1).priceAtAddition()).isEqualTo(Money.cop(new BigDecimal("3.00")));
    }

    @Test
    void shouldFindCartByCustomerId() {
        UUID customerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        Cart own = cartRepository.save(cart(customerId, List.of(milk())));
        cartRepository.save(cart(otherCustomerId, List.of(bread())));

        assertThat(cartRepository.findByCustomerId(customerId))
                .hasValueSatisfying(loaded -> assertThat(loaded.id()).isEqualTo(own.id()));
        assertThat(cartRepository.findByCustomerId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldRejectInvalidQuantityAtDatabase() {
        UUID cartId = UUID.randomUUID();
        insertCartHeader(cartId, UUID.randomUUID());

        assertThatThrownBy(() -> insertItemBypassingDomain(cartId, 0, new BigDecimal("10.50"), "COP"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNegativeMoneyAtDatabase() {
        UUID cartId = UUID.randomUUID();
        insertCartHeader(cartId, UUID.randomUUID());

        assertThatThrownBy(() -> insertItemBypassingDomain(cartId, 1, new BigDecimal("-0.01"), "COP"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectInvalidStatusAtDatabase() {
        assertThatThrownBy(() -> insertCartHeaderWithStatus(UUID.randomUUID(), UUID.randomUUID(), "CHECKED_OUT"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNonCopCurrencyAtDatabase() {
        UUID cartId = UUID.randomUUID();
        insertCartHeader(cartId, UUID.randomUUID());

        assertThatThrownBy(() -> insertItemBypassingDomain(cartId, 1, new BigDecimal("10.50"), "USD"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectItemWhenCartDoesNotExist() {
        assertThatThrownBy(() -> insertItemBypassingDomain(UUID.randomUUID(), 1, new BigDecimal("10.50"), "COP"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Cart cart(UUID customerId, List<CartItem> items) {
        return Cart.reconstitute(
                UUID.randomUUID(), customerId, CartStatus.ACTIVE, items, CREATED_AT, UPDATED_AT);
    }

    private static CartItem milk() {
        return CartItem.create(
                UUID.randomUUID(),
                PRODUCT_ID,
                2,
                Money.cop(new BigDecimal("10.50")),
                ADDED_AT,
                ITEM_UPDATED_AT);
    }

    private static CartItem bread() {
        return CartItem.create(
                UUID.randomUUID(), SECOND_PRODUCT_ID, 1, Money.cop(new BigDecimal("3.00")), ADDED_AT, ADDED_AT);
    }

    private void insertCartHeader(UUID cartId, UUID customerId) {
        insertCartHeaderWithStatus(cartId, customerId, "ACTIVE");
    }

    private void insertCartHeaderWithStatus(UUID cartId, UUID customerId, String status) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into shopping.carts (
                                id, customer_id, status, created_at, updated_at
                            ) values (?, ?, ?, ?, ?)
                            """);
                    statement.setObject(1, cartId);
                    statement.setObject(2, customerId);
                    statement.setString(3, status);
                    statement.setTimestamp(4, Timestamp.from(CREATED_AT));
                    statement.setTimestamp(5, Timestamp.from(CREATED_AT));
                    return statement;
                });
    }

    private void insertItemBypassingDomain(UUID cartId, int quantity, BigDecimal unitPrice, String currency) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into shopping.cart_items (
                                id, cart_id, item_index, product_id, quantity,
                                price_at_addition_amount, price_at_addition_currency,
                                added_at, updated_at
                            ) values (?, ?, 0, ?, ?, ?, ?, ?, ?)
                            """);
                    statement.setObject(1, UUID.randomUUID());
                    statement.setObject(2, cartId);
                    statement.setObject(3, PRODUCT_ID);
                    statement.setInt(4, quantity);
                    statement.setBigDecimal(5, unitPrice);
                    statement.setString(6, currency);
                    statement.setTimestamp(7, Timestamp.from(ADDED_AT));
                    statement.setTimestamp(8, Timestamp.from(ADDED_AT));
                    return statement;
                });
    }
}
