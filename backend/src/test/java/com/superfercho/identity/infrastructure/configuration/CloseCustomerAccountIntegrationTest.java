package com.superfercho.identity.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.assistant.application.port.out.ConversationStore;
import com.superfercho.assistant.domain.model.Conversation;
import com.superfercho.assistant.domain.model.Message;
import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.usecase.CloseCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.RegisterCustomerUseCase;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.identity.infrastructure.security.AuthenticatedUserPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CloseCustomerAccountIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");

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
        registry.add("spring.task.scheduling.enabled", () -> "false");
        registry.add(
                "superfercho.security.jwt.secret", () -> "test-only-superfercho-jwt-secret-key-32b");
    }

    @Autowired
    private RegisterCustomerUseCase registerCustomerUseCase;

    @Autowired
    private CloseCustomerAccountUseCase closeCustomerAccountUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRecordRepository customerRecordRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;

    @Autowired
    private ConversationStore conversationStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSoftCloseCustomerKeepHistoryAndClearOperationalData() {
        RegisteredCustomer registered = registerCustomerUseCase.execute(new RegisterCustomerCommand(
                "CC",
                "77889900",
                "Ada",
                "Lovelace",
                "close-me@example.com",
                "3001234567",
                "Luis123!"));
        UUID customerId = registered.id();
        User before = userRepository.findById(customerId).orElseThrow();
        UUID recordId = before.customerRecordId();
        CustomerRecord recordBefore = customerRecordRepository.findById(recordId).orElseThrow();

        addressRepository.save(customerId, Address.create(
                UUID.randomUUID(),
                "Casa",
                "Ada Lovelace",
                "Calle 1 # 2-3",
                null,
                "Bogotá",
                "Cundinamarca",
                "3001234567",
                true,
                AddressStatus.ACTIVE,
                NOW,
                NOW));
        passwordRecoveryTokenRepository.save(PasswordRecoveryToken.create(
                UUID.randomUUID(),
                customerId,
                "recovery-hash-" + customerId,
                "127.0.0.1",
                NOW,
                NOW.plusSeconds(900)));

        UUID productId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        seedOtherCustomer(otherCustomerId);
        seedShopping(customerId, productId, cartId, listId);
        seedShopping(otherCustomerId, productId, UUID.randomUUID(), UUID.randomUUID());
        seedOrderPaymentAndIdempotency(customerId, orderId, paymentId, productId);

        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.start(conversationId, customerId, NOW)
                .append(Message.user(UUID.randomUUID(), "hola", NOW), NOW);
        conversationStore.save(conversation);

        authenticate(customerId);
        closeCustomerAccountUseCase.execute();

        User closed = userRepository.findById(customerId).orElseThrow();
        assertThat(closed.status()).isEqualTo(UserStatus.INACTIVE);
        assertThat(closed.deletedAt()).isNotNull();
        assertThat(closed.customerRecordId()).isEqualTo(recordId);
        assertThat(closed.email()).isEqualTo("close-me@example.com");

        CustomerRecord recordAfter = customerRecordRepository.findById(recordId).orElseThrow();
        assertThat(recordAfter.billingFirstName()).isEqualTo(recordBefore.billingFirstName());
        assertThat(recordAfter.billingLastName()).isEqualTo(recordBefore.billingLastName());
        assertThat(recordAfter.documentNumber()).isEqualTo("77889900");

        assertThat(addressRepository.findByUserId(customerId)).isEmpty();
        assertThat(passwordRecoveryTokenRepository.findByUserIdCreatedAtOrAfter(customerId, NOW.minusSeconds(1)))
                .isEmpty();
        assertThat(count("select count(*) from shopping.carts where customer_id = ?", customerId)).isZero();
        assertThat(count("select count(*) from shopping.favorites where customer_id = ?", customerId)).isZero();
        assertThat(count("select count(*) from shopping.shopping_lists where customer_id = ?", customerId))
                .isZero();
        assertThat(count(
                        "select count(*) from orders.checkout_idempotency where customer_id = ?",
                        customerId))
                .isZero();
        assertThat(conversationStore.findById(conversationId)).isEmpty();

        assertThat(count("select count(*) from orders.orders where id = ?", orderId)).isEqualTo(1);
        assertThat(count("select count(*) from orders.order_items where order_id = ?", orderId)).isEqualTo(1);
        assertThat(count("select count(*) from payments.payments where id = ?", paymentId)).isEqualTo(1);

        assertThat(count("select count(*) from shopping.carts where customer_id = ?", otherCustomerId))
                .isEqualTo(1);
        assertThat(count("select count(*) from shopping.favorites where customer_id = ?", otherCustomerId))
                .isEqualTo(1);

        closeCustomerAccountUseCase.execute();
        assertThat(userRepository.findById(customerId).orElseThrow().deletedAt()).isEqualTo(closed.deletedAt());

        RegisteredCustomer reopened = registerCustomerUseCase.execute(new RegisterCustomerCommand(
                "CC",
                "77889900",
                "Ada",
                "Reloaded",
                "close-me-reopened@example.com",
                "3009999999",
                "Nuevo123!"));
        User newUser = userRepository.findById(reopened.id()).orElseThrow();
        assertThat(newUser.id()).isNotEqualTo(customerId);
        assertThat(newUser.customerRecordId()).isEqualTo(recordId);
        assertThat(newUser.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(newUser.deletedAt()).isNull();
        assertThat(addressRepository.findByUserId(newUser.id())).isEmpty();
        assertThat(count("select count(*) from shopping.carts where customer_id = ?", newUser.id())).isZero();
    }

    private void authenticate(UUID userId) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(userId, Role.CUSTOMER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private void seedOtherCustomer(UUID otherCustomerId) {
        jdbcTemplate.update(
                """
                insert into identity.users (
                    id, document_type, document_number, first_name, last_name, email, phone,
                    password_hash, role, status, customer_record_id, deleted_at, created_at, updated_at)
                values (?, 'CC', ?, 'Other', 'User', ?, '3002222222', 'hash', 'CUSTOMER', 'ACTIVE',
                        null, null, ?, ?)
                """,
                otherCustomerId,
                "OTHER" + otherCustomerId.toString().replace("-", "").substring(0, 10),
                "other-" + otherCustomerId + "@example.com",
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW));
    }

    private void seedShopping(UUID customerId, UUID productId, UUID cartId, UUID listId) {
        jdbcTemplate.update(
                """
                insert into shopping.carts (id, customer_id, status, created_at, updated_at)
                values (?, ?, 'ACTIVE', ?, ?)
                """,
                cartId,
                customerId,
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW));
        jdbcTemplate.update(
                """
                insert into shopping.cart_items (
                    id, cart_id, item_index, product_id, quantity,
                    price_at_addition_amount, price_at_addition_currency, added_at, updated_at)
                values (?, ?, 0, ?, 1, ?, 'COP', ?, ?)
                """,
                UUID.randomUUID(),
                cartId,
                productId,
                new BigDecimal("10.00"),
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW));
        jdbcTemplate.update(
                """
                insert into shopping.favorites (id, customer_id, product_id, created_at)
                values (?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                customerId,
                productId,
                java.sql.Timestamp.from(NOW));
        jdbcTemplate.update(
                """
                insert into shopping.shopping_lists (id, customer_id, name, created_at, updated_at)
                values (?, ?, 'Lista', ?, ?)
                """,
                listId,
                customerId,
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW));
        jdbcTemplate.update(
                """
                insert into shopping.shopping_list_items (
                    id, shopping_list_id, item_index, product_id, quantity, created_at)
                values (?, ?, 0, ?, 2, ?)
                """,
                UUID.randomUUID(),
                listId,
                productId,
                java.sql.Timestamp.from(NOW));
    }

    private void seedOrderPaymentAndIdempotency(
            UUID customerId, UUID orderId, UUID paymentId, UUID productId) {
        jdbcTemplate.update(
                """
                insert into orders.orders (
                    id, order_number, customer_id, status,
                    subtotal_amount, subtotal_currency, total_amount, total_currency, payment_id,
                    shipping_recipient_name, shipping_address_line, shipping_additional_info,
                    shipping_city, shipping_department, shipping_phone,
                    created_at, confirmed_at, cancelled_at, updated_at)
                values (?, ?, ?, 'CONFIRMED', ?, 'COP', ?, 'COP', ?,
                        'Ada', 'Calle 1', null, 'Bogotá', 'Cundinamarca', '3001234567',
                        ?, ?, null, ?)
                """,
                orderId,
                "ORD-" + orderId.toString().substring(0, 8),
                customerId,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                paymentId,
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW));
        jdbcTemplate.update(
                """
                insert into orders.order_items (
                    id, order_id, item_index, product_id, product_name,
                    unit_price_amount, unit_price_currency, quantity,
                    subtotal_amount, subtotal_currency)
                values (?, ?, 0, ?, 'Milk', ?, 'COP', 1, ?, 'COP')
                """,
                UUID.randomUUID(),
                orderId,
                productId,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"));
        jdbcTemplate.update(
                """
                insert into payments.payments (
                    id, order_id, amount, currency, payment_method, status,
                    provider_reference, created_at, updated_at, refunded_at)
                values (?, ?, ?, 'COP', 'CASH_ON_DELIVERY', 'APPROVED', null, ?, ?, null)
                """,
                paymentId,
                orderId,
                new BigDecimal("10.00"),
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW));
        jdbcTemplate.update(
                """
                insert into orders.checkout_idempotency (
                    id, customer_id, idempotency_key, fingerprint,
                    result_order_id, result_order_number, result_order_status, result_payment_status,
                    result_total_amount, result_total_currency, created_at, expires_at)
                values (?, ?, 'key-1', 'fp-1', ?, 'ORD-X', 'CONFIRMED', 'APPROVED',
                        ?, 'COP', ?, ?)
                """,
                UUID.randomUUID(),
                customerId,
                orderId,
                new BigDecimal("10.00"),
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW.plusSeconds(3600)));
    }

    private int count(String sql, UUID id) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return value == null ? 0 : value;
    }
}
