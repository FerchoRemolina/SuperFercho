package com.superfercho.orders.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

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
import com.superfercho.orders.application.dto.UpdateOrderStatusCommand;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.exception.OrderOwnershipException;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.usecase.UpdateOrderStatusUseCase;
import com.superfercho.orders.domain.exception.InvalidOrderStateTransitionException;
import com.superfercho.orders.domain.exception.OrderCancellationNotAllowedException;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.payments.application.port.PaymentRepository;
import com.superfercho.payments.domain.model.Payment;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartItem;
import com.superfercho.shopping.domain.model.CartStatus;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
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
class TransactionalCancelOrderUseCaseIntegrationTest {

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
        registry.add("superfercho.security.jwt.secret", () -> "test-only-superfercho-jwt-secret-key-32b");
    }

    @Autowired
    private TransactionalCancelOrderUseCase transactionalCancelOrderUseCase;

    @Autowired
    private TransactionalCheckoutUseCase transactionalCheckoutUseCase;

    @Autowired
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

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
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCancelPendingOrderWithApprovedPaymentAndRestoreStock() {
        PreparedOrder prepared = checkoutWith(PaymentMethod.SIMULATED_CARD);
        int stockAfterCheckout = productRepository.findById(prepared.productId()).orElseThrow().stock();
        assertThat(stockAfterCheckout).isEqualTo(INITIAL_STOCK - QUANTITY);

        OrderResult result = executeCancel(prepared.customerId(), prepared.orderId());

        Order order = orderRepository.findById(prepared.orderId()).orElseThrow();
        Payment payment = paymentRepository.findById(prepared.paymentId()).orElseThrow();
        int stockFinal = productRepository.findById(prepared.productId()).orElseThrow().stock();
        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.cancelledAt()).isNotNull();
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancelledAt()).isNotNull();
        assertThat(order.cancelledAt()).isCloseTo(result.cancelledAt(), within(Duration.ofMillis(1)));
        assertThat(order.confirmedAt()).isNull();
        assertThat(payment.status()).isEqualTo(com.superfercho.payments.domain.model.PaymentStatus.APPROVED);
        assertThat(payment.refundedAt()).isNotNull();
        assertThat(payment.refundedAt()).isAfterOrEqualTo(payment.createdAt());
        assertThat(stockFinal).isEqualTo(stockAfterCheckout + QUANTITY);
        assertThat(stockFinal).isEqualTo(INITIAL_STOCK);
        assertThat(cartRepository.findByCustomerId(prepared.customerId()).orElseThrow().items()).isEmpty();
        assertThat(countOrders(prepared.customerId())).isEqualTo(1);
        assertThat(countOrderItems(prepared.orderId())).isEqualTo(1);
        assertThat(countPaymentsForOrder(prepared.orderId())).isEqualTo(1);
        assertThat(countIdempotency(prepared.customerId())).isEqualTo(1);
        assertThat(countOrphanPayments()).isEqualTo(0);
    }

    @Test
    void shouldCancelPendingOrderWithPendingPaymentWithoutRefunding() {
        PreparedOrder prepared = checkoutWith(PaymentMethod.CASH_ON_DELIVERY);
        Payment before = paymentRepository.findById(prepared.paymentId()).orElseThrow();
        int stockAfterCheckout = productRepository.findById(prepared.productId()).orElseThrow().stock();
        assertThat(stockAfterCheckout).isEqualTo(INITIAL_STOCK - QUANTITY);

        OrderResult result = executeCancel(prepared.customerId(), prepared.orderId());

        Order order = orderRepository.findById(prepared.orderId()).orElseThrow();
        Payment payment = paymentRepository.findById(prepared.paymentId()).orElseThrow();
        int stockFinal = productRepository.findById(prepared.productId()).orElseThrow().stock();
        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stockFinal).isEqualTo(stockAfterCheckout + QUANTITY);
        assertThat(stockFinal).isEqualTo(INITIAL_STOCK);
        assertThat(payment.status()).isEqualTo(com.superfercho.payments.domain.model.PaymentStatus.PENDING);
        assertThat(payment.refundedAt()).isNull();
        assertThat(payment.updatedAt()).isEqualTo(before.updatedAt());
        assertThat(countPaymentsForOrder(prepared.orderId())).isEqualTo(1);
        assertThat(countOrphanPayments()).isEqualTo(0);
    }

    @Test
    void shouldRejectCancellationWhenOrderIsNotPending() {
        PreparedOrder prepared = checkoutWith(PaymentMethod.SIMULATED_CARD);
        updateOrderStatusUseCase.execute(new UpdateOrderStatusCommand(prepared.orderId(), OrderStatus.CONFIRMED));
        PaymentSnapshot paymentBefore = paymentSnapshot(prepared.paymentId());
        int stockBefore = productRepository.findById(prepared.productId()).orElseThrow().stock();

        assertThatThrownBy(() -> executeCancel(prepared.customerId(), prepared.orderId()))
                .isInstanceOf(InvalidOrderStateTransitionException.class);

        Order order = orderRepository.findById(prepared.orderId()).orElseThrow();
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.cancelledAt()).isNull();
        assertThat(productRepository.findById(prepared.productId()).orElseThrow().stock()).isEqualTo(stockBefore);
        assertPaymentUnchanged(prepared.paymentId(), paymentBefore);
    }

    @Test
    void shouldRejectCancellationWhenCustomerWindowHasExpired() {
        PreparedOrder prepared = checkoutWith(PaymentMethod.SIMULATED_CARD);
        ageOrderBeyondCancellationWindow(prepared.orderId());
        PaymentSnapshot paymentBefore = paymentSnapshot(prepared.paymentId());
        int stockBefore = productRepository.findById(prepared.productId()).orElseThrow().stock();

        assertThatThrownBy(() -> executeCancel(prepared.customerId(), prepared.orderId()))
                .isInstanceOf(OrderCancellationNotAllowedException.class);

        Order order = orderRepository.findById(prepared.orderId()).orElseThrow();
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.cancelledAt()).isNull();
        assertThat(productRepository.findById(prepared.productId()).orElseThrow().stock()).isEqualTo(stockBefore);
        assertPaymentUnchanged(prepared.paymentId(), paymentBefore);
        assertThat(paymentBefore.refundedAt()).isNull();
    }

    @Test
    void shouldRejectCancellationWhenOrderDoesNotExist() {
        Fixture customer = prepareCustomerWithAddressAndProduct(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);
        authenticate(customer.customerId());
        UUID missingOrderId = UUID.randomUUID();
        int stockBefore = productRepository.findById(customer.productId()).orElseThrow().stock();
        int paymentsBefore = countPayments();

        assertThatThrownBy(() -> executeCancel(customer.customerId(), missingOrderId))
                .isInstanceOf(OrderNotFoundException.class);

        assertThat(countOrders(customer.customerId())).isEqualTo(0);
        assertThat(countPayments()).isEqualTo(paymentsBefore);
        assertThat(productRepository.findById(customer.productId()).orElseThrow().stock()).isEqualTo(stockBefore);
        assertThat(countOrphanPayments()).isEqualTo(0);
    }

    @Test
    void shouldRejectCancellationOfAnotherCustomersOrder() {
        PreparedOrder owner = checkoutWith(PaymentMethod.SIMULATED_CARD);
        Fixture otherCustomer = prepareCustomerWithAddressAndProduct(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);
        PaymentSnapshot paymentBefore = paymentSnapshot(owner.paymentId());
        int stockBefore = productRepository.findById(owner.productId()).orElseThrow().stock();

        assertThatThrownBy(() -> executeCancel(otherCustomer.customerId(), owner.orderId()))
                .isInstanceOf(OrderOwnershipException.class);

        Order order = orderRepository.findById(owner.orderId()).orElseThrow();
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.customerId()).isEqualTo(owner.customerId());
        assertThat(order.cancelledAt()).isNull();
        assertThat(productRepository.findById(owner.productId()).orElseThrow().stock()).isEqualTo(stockBefore);
        assertPaymentUnchanged(owner.paymentId(), paymentBefore);
        assertThat(countOrders(owner.customerId())).isEqualTo(1);
        assertThat(countOrders(otherCustomer.customerId())).isEqualTo(0);
    }

    @Test
    void shouldPersistApprovedPaymentRefundThroughPaymentsIntegration() {
        PreparedOrder prepared = checkoutWith(PaymentMethod.SIMULATED_CARD);
        PaymentSnapshot before = paymentSnapshot(prepared.paymentId());
        assertThat(before.status()).isEqualTo("APPROVED");
        assertThat(before.refundedAt()).isNull();

        executeCancel(prepared.customerId(), prepared.orderId());

        PaymentSnapshot after = paymentSnapshot(prepared.paymentId());
        assertThat(after.status()).isEqualTo("APPROVED");
        assertThat(after.refundedAt()).isNotNull();
        assertThat(after.updatedAt()).isEqualTo(after.refundedAt());
        assertThat(after.updatedAt()).isAfterOrEqualTo(before.updatedAt());
        assertThat(after.refundedAt()).isAfterOrEqualTo(before.createdAt());
        assertThat(countPaymentsForOrder(prepared.orderId())).isEqualTo(1);
        assertThat(countOrphanPayments()).isEqualTo(0);
        assertThat(orderRepository.findById(prepared.orderId()).orElseThrow().status())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    private PreparedOrder checkoutWith(PaymentMethod paymentMethod) {
        Fixture fixture = prepareReadyCheckout();
        CheckoutResult checkout = transactionalCheckoutUseCase.execute(checkoutCommand(fixture, paymentMethod));
        Order order = orderRepository.findById(checkout.orderId()).orElseThrow();
        return new PreparedOrder(
                fixture.customerId(),
                fixture.productId(),
                order.id(),
                order.paymentId());
    }

    private Fixture prepareReadyCheckout() {
        Fixture fixture = prepareCustomerWithAddressAndProduct(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);
        saveCartWithProduct(fixture.customerId(), fixture.productId(), PRICE, QUANTITY);
        authenticate(fixture.customerId());
        return fixture;
    }

    private Fixture prepareCustomerWithAddressAndProduct(int stock, Money price, ProductStatus status) {
        UUID customerId = UUID.randomUUID();
        User user = userRepository.save(newUser(customerId));
        Address address = addressRepository.save(user.id(), newAddress());
        Product product = persistProduct(stock, price, status);
        return new Fixture(user.id(), address.id(), product.id(), UUID.randomUUID().toString());
    }

    private Product persistProduct(int stock, Money price, ProductStatus status) {
        Category category = categoryRepository.save(newCategory());
        ProductType type = productTypeRepository.save(ProductType.create(
                UUID.randomUUID(),
                category.id(),
                "CancelType-" + UUID.randomUUID().toString().substring(0, 8),
                null,
                ProductTypeStatus.ACTIVE,
                NOW,
                NOW));
        return productRepository.save(newProduct(category.id(), type.id(), stock, price, status));
    }

    private OrderResult executeCancel(UUID customerId, UUID orderId) {
        authenticate(customerId);
        return transactionalCancelOrderUseCase.execute(new CancelOrderCommand(orderId));
    }

    private void ageOrderBeyondCancellationWindow(UUID orderId) {
        Instant expiredCreatedAt =
                clock.instant().minus(Order.CUSTOMER_CANCELLATION_WINDOW).minusMillis(1);
        jdbcTemplate.update(
                "update orders.orders set created_at = ? where id = ?",
                Timestamp.from(expiredCreatedAt),
                orderId);
    }

    private void saveCartWithProduct(UUID customerId, UUID productId, Money price, int quantity) {
        cartRepository.save(Cart.create(
                UUID.randomUUID(),
                customerId,
                CartStatus.ACTIVE,
                List.of(CartItem.create(UUID.randomUUID(), productId, quantity, price, NOW, NOW)),
                NOW,
                NOW));
    }

    private static CheckoutCommand checkoutCommand(Fixture fixture, PaymentMethod paymentMethod) {
        return new CheckoutCommand(
                fixture.addressId(),
                paymentMethod,
                List.of(new CheckoutItem(fixture.productId(), QUANTITY, PRICE)),
                fixture.idempotencyKey());
    }

    private void authenticate(UUID customerId) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(customerId, Role.CUSTOMER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private PaymentSnapshot paymentSnapshot(UUID paymentId) {
        return jdbcTemplate.queryForObject(
                """
                select status, created_at, updated_at, refunded_at
                  from payments.payments
                 where id = ?
                """,
                (rs, rowNum) -> new PaymentSnapshot(
                        rs.getString("status"),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at")),
                        toInstant(rs.getTimestamp("refunded_at"))),
                paymentId);
    }

    private void assertPaymentUnchanged(UUID paymentId, PaymentSnapshot before) {
        PaymentSnapshot after = paymentSnapshot(paymentId);
        assertThat(after.status()).isEqualTo(before.status());
        assertThat(after.createdAt()).isEqualTo(before.createdAt());
        assertThat(after.updatedAt()).isEqualTo(before.updatedAt());
        assertThat(after.refundedAt()).isEqualTo(before.refundedAt());
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private int countOrders(UUID customerId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from orders.orders where customer_id = ?", Integer.class, customerId);
        return count == null ? 0 : count;
    }

    private int countOrderItems(UUID orderId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from orders.order_items where order_id = ?", Integer.class, orderId);
        return count == null ? 0 : count;
    }

    private int countPayments() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from payments.payments", Integer.class);
        return count == null ? 0 : count;
    }

    private int countPaymentsForOrder(UUID orderId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from payments.payments where order_id = ?", Integer.class, orderId);
        return count == null ? 0 : count;
    }

    private int countIdempotency(UUID customerId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from orders.checkout_idempotency where customer_id = ?",
                Integer.class,
                customerId);
        return count == null ? 0 : count;
    }

    private int countOrphanPayments() {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*) from payments.payments p
                 where not exists (select 1 from orders.orders o where o.id = p.order_id)
                """,
                Integer.class);
        return count == null ? 0 : count;
    }

    private static User newUser(UUID id) {
        String token = id.toString().replace("-", "");
        return User.create(
                id,
                "CC",
                token.substring(0, 16),
                "Ada Lovelace",
                token + "@cancel-it.test",
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

    private static Category newCategory() {
        return Category.create(
                UUID.randomUUID(), "Cancel-" + UUID.randomUUID(), "Fresh produce", CategoryStatus.ACTIVE, NOW, NOW);
    }

    private static Product newProduct(UUID categoryId, UUID productTypeId, int stock, Money price, ProductStatus status) {
        return Product.create(
                UUID.randomUUID(),
                categoryId,
                productTypeId,
                null,
                Presentation.of(1, PresentationUnit.UNIT),
                null,
                "Leche entera",
                "Alpina",
                "1L",
                price,
                stock,
                null,
                status,
                NOW,
                NOW);
    }

    private record Fixture(UUID customerId, UUID addressId, UUID productId, String idempotencyKey) {}

    private record PreparedOrder(UUID customerId, UUID productId, UUID orderId, UUID paymentId) {}

    private record PaymentSnapshot(String status, Instant createdAt, Instant updatedAt, Instant refundedAt) {}
}
