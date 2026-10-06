package com.superfercho.orders.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryIcon;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.usecase.FinalizeStorefrontPreviewUseCase;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.identity.infrastructure.security.AuthenticatedUserPrincipal;
import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutItem;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.usecase.AdvanceOrderLifecycleUseCase;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartItem;
import com.superfercho.shopping.domain.model.CartStatus;
import java.math.BigDecimal;
import java.sql.Timestamp;
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
class PreviewCheckoutInventoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));
    private static final int QUANTITY = 3;
    private static final int INITIAL_STOCK = 10;

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
    private TransactionalCheckoutUseCase transactionalCheckoutUseCase;

    @Autowired
    private AdvanceOrderLifecycleUseCase advanceOrderLifecycleUseCase;

    @Autowired
    private FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CustomerPreviewRepository customerPreviewRepository;

    @Autowired
    private PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepositoryPort cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void previewCheckoutLifecycleAndFinalizeNeverMutatePersistentStock() {
        UUID adminId = UUID.randomUUID();
        userRepository.save(newUser(adminId, Role.ADMIN, "admin-preview@it.test"));
        UUID temporaryCustomerId = UUID.randomUUID();
        userRepository.save(newUser(temporaryCustomerId, Role.CUSTOMER, "temp-preview@it.test"));
        CustomerPreview preview = customerPreviewRepository.save(
                CustomerPreview.start(UUID.randomUUID(), adminId, temporaryCustomerId, NOW));
        Address address = addressRepository.save(temporaryCustomerId, newAddress());
        Product product = persistProduct(INITIAL_STOCK);
        cartRepository.save(Cart.create(
                UUID.randomUUID(),
                temporaryCustomerId,
                CartStatus.ACTIVE,
                List.of(CartItem.create(UUID.randomUUID(), product.id(), QUANTITY, PRICE, NOW, NOW)),
                NOW,
                NOW));
        authenticate(temporaryCustomerId);

        CheckoutResult checkout = transactionalCheckoutUseCase.execute(new CheckoutCommand(
                address.id(),
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(product.id(), QUANTITY, PRICE)),
                UUID.randomUUID().toString()));

        assertThat(checkout.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(productRepository.findById(product.id()).orElseThrow().stock()).isEqualTo(INITIAL_STOCK);

        backdateConfirmedAt(checkout.orderId(), Instant.now().minusSeconds(400));
        assertThat(advanceOrderLifecycleUseCase.execute())
                .anySatisfy(result -> {
                    assertThat(result.id()).isEqualTo(checkout.orderId());
                    assertThat(result.status()).isEqualTo(OrderStatus.DELIVERED);
                });
        Order delivered = orderRepository.findById(checkout.orderId()).orElseThrow();
        assertThat(delivered.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(productRepository.findById(product.id()).orElseThrow().stock()).isEqualTo(INITIAL_STOCK);

        passwordRecoveryTokenRepository.save(PasswordRecoveryToken.create(
                UUID.randomUUID(),
                temporaryCustomerId,
                "preview-recovery-hash-" + temporaryCustomerId,
                "127.0.0.1",
                Instant.now(),
                Instant.now().plusSeconds(3600)));
        assertThat(countRecoveryTokens(temporaryCustomerId)).isEqualTo(1);

        assertThat(finalizeStorefrontPreviewUseCase.execute(preview.id())).isTrue();
        assertThat(finalizeStorefrontPreviewUseCase.execute(preview.id())).isFalse();

        assertThat(customerPreviewRepository.findById(preview.id())).isEmpty();
        assertThat(userRepository.findById(temporaryCustomerId)).isEmpty();
        assertThat(countRecoveryTokens(temporaryCustomerId)).isEqualTo(0);
        assertThat(countOrders(temporaryCustomerId)).isEqualTo(0);
        assertThat(productRepository.findById(product.id()).orElseThrow().stock()).isEqualTo(INITIAL_STOCK);
        assertThat(userRepository.findById(adminId)).isPresent();
    }

    @Test
    void realCustomerCheckoutStillDecrementsPersistentStock() {
        UUID customerId = UUID.randomUUID();
        userRepository.save(newUser(customerId, Role.CUSTOMER, "real-customer@it.test"));
        Address address = addressRepository.save(customerId, newAddress());
        Product product = persistProduct(INITIAL_STOCK);
        cartRepository.save(Cart.create(
                UUID.randomUUID(),
                customerId,
                CartStatus.ACTIVE,
                List.of(CartItem.create(UUID.randomUUID(), product.id(), QUANTITY, PRICE, NOW, NOW)),
                NOW,
                NOW));
        authenticate(customerId);

        CheckoutResult checkout = transactionalCheckoutUseCase.execute(new CheckoutCommand(
                address.id(),
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(product.id(), QUANTITY, PRICE)),
                UUID.randomUUID().toString()));

        assertThat(checkout.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(productRepository.findById(product.id()).orElseThrow().stock())
                .isEqualTo(INITIAL_STOCK - QUANTITY);
    }

    private Product persistProduct(int stock) {
        Category category = categoryRepository.save(Category.create(
                UUID.randomUUID(),
                "Preview-" + UUID.randomUUID(),
                "Fresh produce",
                CategoryIcon.OTHER, CategoryStatus.ACTIVE,
                NOW,
                NOW));
        ProductType type = productTypeRepository.save(ProductType.create(
                UUID.randomUUID(),
                category.id(),
                "PreviewType-" + UUID.randomUUID().toString().substring(0, 8),
                null,
                ProductTypeStatus.ACTIVE,
                NOW,
                NOW));
        return productRepository.save(Product.create(
                UUID.randomUUID(),
                category.id(),
                type.id(),
                null,
                Presentation.of(1, PresentationUnit.UNIT),
                null,
                "Leche entera",
                "Alpina",
                "1L",
                PRICE,
                stock,
                null,
                ProductStatus.ACTIVE,
                NOW,
                NOW));
    }

    private void backdateConfirmedAt(UUID orderId, Instant aged) {
        jdbcTemplate.update(
                """
                update orders.orders
                   set created_at = ?,
                       confirmed_at = ?,
                       updated_at = ?
                 where id = ?
                """,
                Timestamp.from(aged),
                Timestamp.from(aged),
                Timestamp.from(aged),
                orderId);
    }

    private int countRecoveryTokens(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from identity.password_recovery_tokens where user_id = ?",
                Integer.class,
                userId);
        return count == null ? 0 : count;
    }

    private int countOrders(UUID customerId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from orders.orders where customer_id = ?", Integer.class, customerId);
        return count == null ? 0 : count;
    }

    private void authenticate(UUID customerId) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(customerId, Role.CUSTOMER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private static User newUser(UUID id, Role role, String email) {
        String token = id.toString().replace("-", "");
        return User.create(
                id,
                "CC",
                token.substring(0, 16),
                "Ada",
                "Lovelace",
                email,
                "3001234567",
                "hashed-password",
                role,
                UserStatus.ACTIVE,
                NOW,
                NOW);
    }

    private static Address newAddress() {
        return Address.create(
                UUID.randomUUID(),
                "Casa",
                "Ada Lovelace",
                "Calle 1 # 2-3",
                "Apto 101",
                "Bogotá",
                "Cundinamarca",
                "3001234567",
                true,
                AddressStatus.ACTIVE,
                NOW,
                NOW);
    }
}
