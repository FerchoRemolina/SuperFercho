package com.superfercho.orders.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.identity.infrastructure.security.AuthenticatedUserPrincipal;
import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutItem;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.usecase.AdvanceOrderLifecycleUseCase;
import com.superfercho.orders.domain.exception.InvalidOrderStateTransitionException;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartItem;
import com.superfercho.shopping.domain.model.CartStatus;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.time.Clock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class ConfirmedOrderTransitionConcurrencyIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));
    private static final int QUANTITY = 2;
    private static final int INITIAL_STOCK = 5;

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
        registry.add("superfercho.security.jwt.secret", () -> "test-only-superfercho-jwt-secret-key-32b");
    }

    @Autowired
    private TransactionalCheckoutUseCase transactionalCheckoutUseCase;

    @Autowired
    private TransactionalCancelOrderUseCase transactionalCancelOrderUseCase;

    @Autowired
    private AdvanceOrderLifecycleUseCase advanceOrderLifecycleUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepositoryPort cartRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void clearOrdersBetweenTests() {
        jdbcTemplate.update("delete from orders.order_items");
        jdbcTemplate.update("delete from orders.orders");
        entityManager.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCancelConfirmedOrderWithinWindow() {
        PreparedOrder prepared = checkout();
        int stockAfterCheckout = productStock(prepared.productId());

        OrderResult result = executeCancel(prepared.customerId(), prepared.orderId());

        Order order = orderRepository.findById(prepared.orderId()).orElseThrow();
        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.confirmedAt()).isNull();
        assertThat(productStock(prepared.productId())).isEqualTo(stockAfterCheckout + QUANTITY);
        assertThat(productStock(prepared.productId())).isEqualTo(INITIAL_STOCK);
    }

    @Test
    void shouldAdvanceConfirmedOrderToPreparingOnceItIsOldEnough() {
        PreparedOrder prepared = checkout();
        int stockAfterCheckout = productStock(prepared.productId());
        ageOrder(prepared.orderId(), Order.PREPARING_AFTER);

        List<OrderResult> advanced = advanceOrderLifecycleUseCase.execute();

        Order order = orderRepository.findById(prepared.orderId()).orElseThrow();
        assertThat(advanced).extracting(OrderResult::id).contains(prepared.orderId());
        assertThat(order.status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(order.cancelledAt()).isNull();
        assertThat(order.confirmedAt()).isNotNull();
        assertThat(productStock(prepared.productId())).isEqualTo(stockAfterCheckout);
        assertThat(productStock(prepared.productId())).isEqualTo(INITIAL_STOCK - QUANTITY);
    }

    @Test
    void shouldNotAdvanceAfterSuccessfulCancellation() {
        PreparedOrder prepared = checkout();
        executeCancel(prepared.customerId(), prepared.orderId());
        ageOrder(prepared.orderId(), Order.PREPARING_AFTER);

        List<OrderResult> advanced = advanceOrderLifecycleUseCase.execute();

        Order order = orderRepository.findById(prepared.orderId()).orElseThrow();
        assertThat(advanced).extracting(OrderResult::id).doesNotContain(prepared.orderId());
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.confirmedAt()).isNull();
        assertThat(productStock(prepared.productId())).isEqualTo(INITIAL_STOCK);
    }

    @Test
    void shouldNotCancelAfterOrderLeftConfirmed() {
        PreparedOrder prepared = checkout();
        int stockAfterCheckout = productStock(prepared.productId());
        ageOrder(prepared.orderId(), Order.PREPARING_AFTER);
        advanceOrderLifecycleUseCase.execute();

        assertThatThrownBy(() -> executeCancel(prepared.customerId(), prepared.orderId()))
                .isInstanceOf(InvalidOrderStateTransitionException.class);

        Order order = orderRepository.findById(prepared.orderId()).orElseThrow();
        assertThat(order.status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(order.cancelledAt()).isNull();
        assertThat(productStock(prepared.productId())).isEqualTo(stockAfterCheckout);
    }

    @Test
    void shouldKeepExactlyOneLeavingConfirmedTransitionWhenCancelAndLifecycleRace() throws Exception {
        PreparedOrder prepared = checkout();
        int stockAfterCheckout = productStock(prepared.productId());
        AdvanceOrderLifecycleUseCase futureLifecycle =
                new AdvanceOrderLifecycleUseCase(orderRepository, futureClockPastPreparingWindow());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> cancel = executor.submit(() -> {
                await(start);
                authenticate(prepared.customerId());
                try {
                    transactionalCancelOrderUseCase.execute(new CancelOrderCommand(prepared.orderId()));
                } catch (InvalidOrderStateTransitionException ignored) {
                    // The lifecycle job already moved the order out of CONFIRMED.
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
            Future<?> advance = executor.submit(() -> {
                await(start);
                futureLifecycle.execute();
            });
            start.countDown();
            cancel.get(20, TimeUnit.SECONDS);
            advance.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        Order order = orderRepository.findById(prepared.orderId()).orElseThrow();
        int stock = productStock(prepared.productId());
        assertThat(order.status()).isIn(OrderStatus.CANCELLED, OrderStatus.PREPARING);
        if (order.status() == OrderStatus.CANCELLED) {
            assertThat(order.confirmedAt()).isNull();
            assertThat(order.cancelledAt()).isNotNull();
            assertThat(stock).isEqualTo(INITIAL_STOCK);
        } else {
            assertThat(order.cancelledAt()).isNull();
            assertThat(order.confirmedAt()).isNotNull();
            assertThat(stock).isEqualTo(stockAfterCheckout);
            assertThat(stock).isEqualTo(INITIAL_STOCK - QUANTITY);
        }
    }

    private PreparedOrder checkout() {
        UUID customerId = UUID.randomUUID();
        User user = userRepository.save(newUser(customerId));
        Address address = addressRepository.save(user.id(), newAddress());
        Product product = persistProduct();
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
        return new PreparedOrder(customerId, product.id(), checkout.orderId());
    }

    private OrderResult executeCancel(UUID customerId, UUID orderId) {
        authenticate(customerId);
        return transactionalCancelOrderUseCase.execute(new CancelOrderCommand(orderId));
    }

    private Product persistProduct() {
        Category category = categoryRepository.save(Category.create(
                UUID.randomUUID(), "Race-" + UUID.randomUUID(), "Fresh produce", CategoryStatus.ACTIVE, NOW, NOW));
        ProductType type = productTypeRepository.save(ProductType.create(
                UUID.randomUUID(),
                category.id(),
                "RaceType-" + UUID.randomUUID().toString().substring(0, 8),
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
                INITIAL_STOCK,
                null,
                ProductStatus.ACTIVE,
                NOW,
                NOW));
    }

    private int productStock(UUID productId) {
        return productRepository.findById(productId).orElseThrow().stock();
    }

    /**
     * Backdates created/confirmed timestamps so the lifecycle use case (system clock) sees the order
     * as {@code age} old. Does not reintroduce {@code confirmed_at} on cancelled rows. Clears the
     * persistence context so subsequent reads see the JDBC update.
     */
    private void ageOrder(UUID orderId, Duration age) {
        Instant aged = clock.instant().minus(age).minusMillis(1);
        jdbcTemplate.update(
                "update orders.orders set created_at = ? where id = ?", Timestamp.from(aged), orderId);
        jdbcTemplate.update(
                "update orders.orders set confirmed_at = ? where id = ? and confirmed_at is not null",
                Timestamp.from(aged),
                orderId);
        entityManager.clear();
    }

    private void authenticate(UUID customerId) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(customerId, Role.CUSTOMER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    /**
     * Clock used only by the lifecycle side of the cancel-vs-advance race: cancel still uses the
     * Spring clock (order still inside the 2-minute window), while advance sees the order as past
     * {@link Order#PREPARING_AFTER} so CAS {@code saveIfConfirmed} decides the winner.
     */
    private ClockProvider futureClockPastPreparingWindow() {
        Instant baseline = clock.instant();
        return () -> baseline.plus(Order.PREPARING_AFTER).plusSeconds(1);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting to start concurrent transitions");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static User newUser(UUID id) {
        String token = id.toString().replace("-", "");
        return User.create(
                id,
                "CC",
                token.substring(0, 16),
                "Ada",
                "Lovelace",
                token + "@confirmed-race.test",
                "3001234567",
                "hashed-password",
                Role.CUSTOMER,
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

    private record PreparedOrder(UUID customerId, UUID productId, UUID orderId) {}
}
