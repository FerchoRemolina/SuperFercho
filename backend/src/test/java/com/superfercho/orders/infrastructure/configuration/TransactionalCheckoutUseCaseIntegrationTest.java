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
import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutItem;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.exception.CartEmptyException;
import com.superfercho.orders.application.exception.IdempotencyConflictException;
import com.superfercho.orders.application.exception.ProductNotAvailableException;
import com.superfercho.orders.application.exception.ProductPriceChangedException;
import com.superfercho.orders.application.exception.StockUnavailableException;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.usecase.CheckoutUseCase;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TransactionalCheckoutUseCaseIntegrationTest {

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
    private TransactionalCheckoutUseCase transactionalCheckoutUseCase;

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
    private IdempotencyPort idempotencyPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCommitSimulatedCardCheckoutAndPersistOrderPaymentStockCartAndIdempotency() {
        Fixture fixture = prepareReadyCheckout(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);

        CheckoutResult result = transactionalCheckoutUseCase.execute(cardCommand(fixture));

        Order order = orderRepository.findById(result.orderId()).orElseThrow();
        Payment payment = paymentRepository.findById(order.paymentId()).orElseThrow();

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.total()).isEqualTo(Money.cop(new BigDecimal("21.00")));
        assertThat(result.orderId()).isEqualTo(order.id());
        assertThat(result.orderNumber()).isEqualTo(order.orderNumber().value());
        assertThat(order.customerId()).isEqualTo(fixture.customerId());
        assertThat(order.items()).hasSize(1);
        assertThat(countOrderItems(order.id())).isEqualTo(1);
        assertThat(payment.orderId()).isEqualTo(order.id());
        assertThat(payment.status()).isEqualTo(com.superfercho.payments.domain.model.PaymentStatus.APPROVED);
        assertThat(payment.providerReference()).isEqualTo("sim-approved");
        assertThat(payment.amount()).isEqualTo(result.total());
        assertThat(productRepository.findById(fixture.productId()).orElseThrow().stock())
                .isEqualTo(INITIAL_STOCK - QUANTITY);
        assertThat(cartRepository.findByCustomerId(fixture.customerId()).orElseThrow().items()).isEmpty();
        assertThat(countIdempotency(fixture.customerId())).isEqualTo(1);
        assertThat(idempotencyPort.find(fixture.idempotencyKey(), fixture.customerId()))
                .hasValueSatisfying(record -> {
                    assertThat(record.result().orderId()).isEqualTo(result.orderId());
                    assertThat(record.result().paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
                });
        assertThat(countOrders(fixture.customerId())).isEqualTo(1);
        assertThat(countPaymentsForOrder(order.id())).isEqualTo(1);
    }

    @Test
    void shouldCommitCashOnDeliveryCheckoutWithPendingPayment() {
        Fixture fixture = prepareReadyCheckout(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);

        CheckoutResult result = transactionalCheckoutUseCase.execute(codCommand(fixture));

        Order order = orderRepository.findById(result.orderId()).orElseThrow();
        Payment payment = paymentRepository.findById(order.paymentId()).orElseThrow();

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.status()).isEqualTo(com.superfercho.payments.domain.model.PaymentStatus.PENDING);
        assertThat(payment.providerReference()).isEqualTo("cod-pending");
        assertThat(payment.refundedAt()).isNull();
        assertThat(payment.orderId()).isEqualTo(order.id());
        assertThat(productRepository.findById(fixture.productId()).orElseThrow().stock())
                .isEqualTo(INITIAL_STOCK - QUANTITY);
        assertThat(cartRepository.findByCustomerId(fixture.customerId()).orElseThrow().items()).isEmpty();
        assertThat(countIdempotency(fixture.customerId())).isEqualTo(1);
        assertThat(countOrders(fixture.customerId())).isEqualTo(1);
    }

    @Test
    void shouldRejectEmptyCartWithoutPersistingCheckoutSideEffects() {
        Fixture fixture = prepareCustomerWithAddressAndProduct(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);
        saveEmptyCart(fixture.customerId());
        authenticate(fixture.customerId());
        DatabaseSnapshot before = snapshot(fixture);

        assertThatThrownBy(() -> transactionalCheckoutUseCase.execute(cardCommand(fixture)))
                .isInstanceOf(CartEmptyException.class);

        assertNoCheckoutSideEffects(fixture, before);
    }

    @Test
    void shouldRejectMissingOrInactiveProductWithoutPersistingCheckoutSideEffects() {
        Fixture fixture = prepareReadyCheckout(INITIAL_STOCK, PRICE, ProductStatus.INACTIVE);
        DatabaseSnapshot before = snapshot(fixture);

        assertThatThrownBy(() -> transactionalCheckoutUseCase.execute(cardCommand(fixture)))
                .isInstanceOf(ProductNotAvailableException.class);

        assertNoCheckoutSideEffects(fixture, before);
        assertThat(cartRepository.findByCustomerId(fixture.customerId()).orElseThrow().items()).hasSize(1);
    }

    @Test
    void shouldRejectProductWithInactiveCategoryWithoutPersistingCheckoutSideEffects() {
        Fixture fixture = prepareReadyCheckout(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);
        deactivateCategoryOf(fixture.productId());
        DatabaseSnapshot before = snapshot(fixture);

        assertThatThrownBy(() -> transactionalCheckoutUseCase.execute(cardCommand(fixture)))
                .isInstanceOf(ProductNotAvailableException.class);

        assertNoCheckoutSideEffects(fixture, before);
        assertThat(cartRepository.findByCustomerId(fixture.customerId()).orElseThrow().items()).hasSize(1);
        assertThat(productRepository.findById(fixture.productId()).orElseThrow().status())
                .isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void shouldRejectInsufficientStockBeforeProcessingPayment() {
        Fixture fixture = prepareReadyCheckout(1, PRICE, ProductStatus.ACTIVE);
        DatabaseSnapshot before = snapshot(fixture);

        assertThatThrownBy(() -> transactionalCheckoutUseCase.execute(cardCommand(fixture)))
                .isInstanceOf(StockUnavailableException.class);

        assertNoCheckoutSideEffects(fixture, before);
        assertThat(cartRepository.findByCustomerId(fixture.customerId()).orElseThrow().items()).hasSize(1);
    }

    @Test
    void shouldRejectPriceChangeWithoutPersistingCheckoutSideEffects() {
        Fixture fixture = prepareReadyCheckout(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);
        DatabaseSnapshot before = snapshot(fixture);
        CheckoutCommand command = new CheckoutCommand(
                fixture.addressId(),
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(fixture.productId(), QUANTITY, Money.cop(new BigDecimal("9.00")))),
                fixture.idempotencyKey());

        assertThatThrownBy(() -> transactionalCheckoutUseCase.execute(command))
                .isInstanceOf(ProductPriceChangedException.class);

        assertNoCheckoutSideEffects(fixture, before);
        assertThat(cartRepository.findByCustomerId(fixture.customerId()).orElseThrow().items()).hasSize(1);
    }

    @Test
    void shouldReplaySameIdempotencyKeyAndFingerprintWithoutDuplicatingOrder() {
        Fixture fixture = prepareReadyCheckout(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);
        CheckoutCommand command = cardCommand(fixture);

        CheckoutResult first = transactionalCheckoutUseCase.execute(command);
        CheckoutResult second = transactionalCheckoutUseCase.execute(command);

        assertThat(second.orderId()).isEqualTo(first.orderId());
        assertThat(second.orderNumber()).isEqualTo(first.orderNumber());
        assertThat(countOrders(fixture.customerId())).isEqualTo(1);
        assertThat(countPaymentsForOrder(first.orderId())).isEqualTo(1);
        assertThat(countIdempotency(fixture.customerId())).isEqualTo(1);
        assertThat(productRepository.findById(fixture.productId()).orElseThrow().stock())
                .isEqualTo(INITIAL_STOCK - QUANTITY);
        assertThat(cartRepository.findByCustomerId(fixture.customerId()).orElseThrow().items()).isEmpty();
        assertThat(countOrphanPayments()).isEqualTo(0);
    }

    @Test
    void shouldConflictWhenIdempotencyKeyIsReusedWithDifferentFingerprint() {
        Fixture fixture = prepareReadyCheckout(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);
        CheckoutResult first = transactionalCheckoutUseCase.execute(cardCommand(fixture));
        CheckoutCommand conflicting = new CheckoutCommand(
                fixture.addressId(),
                PaymentMethod.CASH_ON_DELIVERY,
                List.of(new CheckoutItem(fixture.productId(), QUANTITY, PRICE)),
                fixture.idempotencyKey());

        assertThatThrownBy(() -> transactionalCheckoutUseCase.execute(conflicting))
                .isInstanceOf(IdempotencyConflictException.class);

        assertThat(countOrders(fixture.customerId())).isEqualTo(1);
        assertThat(orderRepository.findById(first.orderId()).orElseThrow().id()).isEqualTo(first.orderId());
        assertThat(countPaymentsForOrder(first.orderId())).isEqualTo(1);
        assertThat(countIdempotency(fixture.customerId())).isEqualTo(1);
        assertThat(productRepository.findById(fixture.productId()).orElseThrow().stock())
                .isEqualTo(INITIAL_STOCK - QUANTITY);
        assertThat(cartRepository.findByCustomerId(fixture.customerId()).orElseThrow().items()).isEmpty();
        assertThat(idempotencyPort.find(fixture.idempotencyKey(), fixture.customerId()))
                .hasValueSatisfying(record ->
                        assertThat(record.result().orderId()).isEqualTo(first.orderId()));
        assertThat(countOrphanPayments()).isEqualTo(0);
    }

    @Test
    void shouldSerializeConcurrentCheckoutsWithSameIdempotencyKey() throws Exception {
        Fixture fixture = prepareReadyCheckout(INITIAL_STOCK, PRICE, ProductStatus.ACTIVE);
        CheckoutCommand command = cardCommand(fixture);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Thread> firstWorker = new AtomicReference<>();
        AtomicReference<Thread> secondWorker = new AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<CheckoutResult> first = pool.submit(
                    () -> executeAuthenticated(fixture.customerId(), command, ready, start, firstWorker));
            Future<CheckoutResult> second = pool.submit(
                    () -> executeAuthenticated(fixture.customerId(), command, ready, start, secondWorker));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(firstWorker.get()).isNotNull();
            assertThat(secondWorker.get()).isNotNull();
            start.countDown();
            requireOverlappingTransactionalCheckout(firstWorker.get(), secondWorker.get());
            CheckoutResult a = first.get(30, TimeUnit.SECONDS);
            CheckoutResult b = second.get(30, TimeUnit.SECONDS);

            assertThat(a.orderId()).isEqualTo(b.orderId());
            assertThat(countOrders(fixture.customerId())).isEqualTo(1);
            assertThat(countPaymentsForOrder(a.orderId())).isEqualTo(1);
            assertThat(countIdempotency(fixture.customerId())).isEqualTo(1);
            assertThat(productRepository.findById(fixture.productId()).orElseThrow().stock())
                    .isEqualTo(INITIAL_STOCK - QUANTITY);
            assertThat(cartRepository.findByCustomerId(fixture.customerId()).orElseThrow().items()).isEmpty();
            assertThat(countOrphanPayments()).isEqualTo(0);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void shouldKeepOneWinnerWhenTwoCheckoutsCompeteForLastStockUnit() throws Exception {
        Product product = persistProduct(1, PRICE, ProductStatus.ACTIVE);
        Fixture firstShopper = prepareShopperForProduct(product, 1);
        Fixture secondShopper = prepareShopperForProduct(product, 1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicReference<CheckoutResult> winner = new AtomicReference<>();
        AtomicReference<Thread> firstWorker = new AtomicReference<>();
        AtomicReference<Thread> secondWorker = new AtomicReference<>();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        try {
            Future<?> firstAttempt = pool.submit(() -> collectCheckout(
                    firstShopper.customerId(),
                    cardCommand(firstShopper, 1),
                    ready,
                    start,
                    firstWorker,
                    winner,
                    failures));
            Future<?> secondAttempt = pool.submit(() -> collectCheckout(
                    secondShopper.customerId(),
                    cardCommand(secondShopper, 1),
                    ready,
                    start,
                    secondWorker,
                    winner,
                    failures));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(firstWorker.get()).isNotNull();
            assertThat(secondWorker.get()).isNotNull();
            start.countDown();
            requireOverlappingTransactionalCheckout(firstWorker.get(), secondWorker.get());
            firstAttempt.get(30, TimeUnit.SECONDS);
            secondAttempt.get(30, TimeUnit.SECONDS);

            assertThat(winner.get()).isNotNull();
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0)).isInstanceOf(StockUnavailableException.class);

            UUID winnerCustomerId = orderRepository.findById(winner.get().orderId()).orElseThrow().customerId();
            UUID loserCustomerId = winnerCustomerId.equals(firstShopper.customerId())
                    ? secondShopper.customerId()
                    : firstShopper.customerId();
            String loserKey = winnerCustomerId.equals(firstShopper.customerId())
                    ? secondShopper.idempotencyKey()
                    : firstShopper.idempotencyKey();

            assertThat(countOrders(firstShopper.customerId()) + countOrders(secondShopper.customerId()))
                    .isEqualTo(1);
            assertThat(countPaymentsForOrder(winner.get().orderId())).isEqualTo(1);
            assertThat(productRepository.findById(product.id()).orElseThrow().stock()).isEqualTo(0);
            assertThat(cartRepository.findByCustomerId(winnerCustomerId).orElseThrow().items()).isEmpty();
            assertThat(cartRepository.findByCustomerId(loserCustomerId).orElseThrow().items()).hasSize(1);
            assertThat(countIdempotency(winnerCustomerId)).isEqualTo(1);
            assertThat(countIdempotency(loserCustomerId)).isEqualTo(0);
            assertThat(idempotencyPort.find(loserKey, loserCustomerId)).isEmpty();
            assertThat(countOrphanPayments()).isEqualTo(0);
        } finally {
            pool.shutdownNow();
        }
    }

    private Fixture prepareReadyCheckout(int stock, Money price, ProductStatus status) {
        Fixture fixture = prepareCustomerWithAddressAndProduct(stock, price, status);
        saveCartWithProduct(fixture.customerId(), fixture.productId(), price, QUANTITY);
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

    private Fixture prepareShopperForProduct(Product product, int quantity) {
        UUID customerId = UUID.randomUUID();
        User user = userRepository.save(newUser(customerId));
        Address address = addressRepository.save(user.id(), newAddress());
        saveCartWithProduct(user.id(), product.id(), product.price(), quantity);
        return new Fixture(user.id(), address.id(), product.id(), UUID.randomUUID().toString());
    }

    private Product persistProduct(int stock, Money price, ProductStatus status) {
        Category category = categoryRepository.save(newCategory());
        ProductType type = productTypeRepository.save(ProductType.create(
                UUID.randomUUID(),
                category.id(),
                "CheckoutType-" + UUID.randomUUID(),
                null,
                ProductTypeStatus.ACTIVE,
                NOW,
                NOW));
        return productRepository.save(newProduct(category.id(), type.id(), stock, price, status));
    }

    private CheckoutResult executeAuthenticated(
            UUID customerId,
            CheckoutCommand command,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicReference<Thread> worker) {
        worker.set(Thread.currentThread());
        authenticate(customerId);
        ready.countDown();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting to start checkout");
            }
            return transactionalCheckoutUseCase.execute(command);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting to start checkout", interrupted);
        }
    }

    private void collectCheckout(
            UUID customerId,
            CheckoutCommand command,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicReference<Thread> worker,
            AtomicReference<CheckoutResult> winner,
            List<Throwable> failures) {
        try {
            CheckoutResult result = executeAuthenticated(customerId, command, ready, start, worker);
            if (!winner.compareAndSet(null, result)) {
                failures.add(new IllegalStateException("more than one checkout succeeded"));
            }
        } catch (RuntimeException exception) {
            failures.add(exception);
        }
    }

    private static void requireOverlappingTransactionalCheckout(Thread firstWorker, Thread secondWorker) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (isInsideTransactionalCheckout(firstWorker) && isInsideTransactionalCheckout(secondWorker)) {
                return;
            }
            Thread.onSpinWait();
        }
        assertThat(isInsideTransactionalCheckout(firstWorker) && isInsideTransactionalCheckout(secondWorker))
                .as("both workers must overlap inside TransactionalCheckoutUseCase.execute; sequential replay is not accepted")
                .isTrue();
    }

    private static boolean isInsideTransactionalCheckout(Thread worker) {
        if (worker == null) {
            return false;
        }
        for (StackTraceElement frame : worker.getStackTrace()) {
            if (!"execute".equals(frame.getMethodName())) {
                continue;
            }
            if (TransactionalCheckoutUseCase.class.getName().equals(frame.getClassName())
                    || CheckoutUseCase.class.getName().equals(frame.getClassName())) {
                return true;
            }
        }
        return false;
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

    private void saveEmptyCart(UUID customerId) {
        cartRepository.save(Cart.create(
                UUID.randomUUID(), customerId, CartStatus.ACTIVE, List.of(), NOW, NOW));
    }

    private void deactivateCategoryOf(UUID productId) {
        Product product = productRepository.findById(productId).orElseThrow();
        Category category = categoryRepository.findById(product.categoryId()).orElseThrow();
        categoryRepository.save(category.deactivate(NOW.plusSeconds(1)));
    }

    private static CheckoutCommand cardCommand(Fixture fixture) {
        return cardCommand(fixture, QUANTITY);
    }

    private static CheckoutCommand cardCommand(Fixture fixture, int quantity) {
        return new CheckoutCommand(
                fixture.addressId(),
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(fixture.productId(), quantity, PRICE)),
                fixture.idempotencyKey());
    }

    private static CheckoutCommand codCommand(Fixture fixture) {
        return new CheckoutCommand(
                fixture.addressId(),
                PaymentMethod.CASH_ON_DELIVERY,
                List.of(new CheckoutItem(fixture.productId(), QUANTITY, PRICE)),
                fixture.idempotencyKey());
    }

    private void authenticate(UUID customerId) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(customerId, Role.CUSTOMER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private DatabaseSnapshot snapshot(Fixture fixture) {
        return new DatabaseSnapshot(
                countOrders(fixture.customerId()),
                countPayments(),
                countIdempotency(fixture.customerId()),
                productRepository.findById(fixture.productId()).orElseThrow().stock());
    }

    private void assertNoCheckoutSideEffects(Fixture fixture, DatabaseSnapshot before) {
        assertThat(countOrders(fixture.customerId())).isEqualTo(before.orders());
        assertThat(countPayments()).isEqualTo(before.payments());
        assertThat(countIdempotency(fixture.customerId())).isEqualTo(before.idempotency());
        assertThat(productRepository.findById(fixture.productId()).orElseThrow().stock()).isEqualTo(before.stock());
        assertThat(countOrphanPayments()).isEqualTo(0);
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
                token + "@checkout-it.test",
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
                UUID.randomUUID(), "Checkout-" + UUID.randomUUID(), "Fresh produce", CategoryStatus.ACTIVE, NOW, NOW);
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

    private record DatabaseSnapshot(int orders, int payments, int idempotency, int stock) {}
}
