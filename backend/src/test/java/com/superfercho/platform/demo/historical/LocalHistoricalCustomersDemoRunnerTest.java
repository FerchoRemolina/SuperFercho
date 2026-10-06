package com.superfercho.platform.demo.historical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import com.superfercho.identity.application.dto.AuthenticateUserCommand;
import com.superfercho.identity.application.dto.AuthenticationResult;
import com.superfercho.identity.application.fakes.FakePasswordHasher;
import com.superfercho.identity.application.fakes.InMemoryAddressRepository;
import com.superfercho.identity.application.fakes.InMemoryCustomerRecordRepository;
import com.superfercho.identity.application.fakes.InMemoryPasswordRecoveryTokenRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.AccessTokenIssuer;
import com.superfercho.identity.application.port.IssuedAccessToken;
import com.superfercho.identity.application.usecase.AuthenticateUserUseCase;
import com.superfercho.identity.application.validation.CustomerRegistrationRules;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.identity.infrastructure.security.BCryptPasswordHasher;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.payments.application.port.PaymentRepository;
import com.superfercho.payments.domain.model.Payment;
import com.superfercho.platform.demo.LocalDemoDataRunner;
import com.superfercho.platform.demo.LocalDemoDataRunner.DatasetProduct;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.Favorite;
import com.superfercho.shopping.domain.model.ShoppingList;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LocalHistoricalCustomersDemoRunnerTest {

    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TYPE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private InMemoryUserRepository userRepository;
    private InMemoryCustomerRecordRepository customerRecordRepository;
    private InMemoryAddressRepository addressRepository;
    private FakePasswordHasher passwordHasher;
    private InMemoryPasswordRecoveryTokenRepository tokenRepository;
    private InMemoryProductRepository productRepository;
    private InMemoryProductVariantRepository variantRepository;
    private InMemoryFavoriteRepository favoriteRepository;
    private InMemoryShoppingListRepository shoppingListRepository;
    private InMemoryCartRepository cartRepository;
    private InMemoryOrderRepository orderRepository;
    private InMemoryPaymentRepository paymentRepository;
    private CountingIdempotencyPort idempotencyPort;
    private LocalHistoricalCustomersDemoRunner runner;
    private Map<UUID, Integer> initialStock;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        customerRecordRepository = new InMemoryCustomerRecordRepository();
        addressRepository = new InMemoryAddressRepository();
        passwordHasher = new FakePasswordHasher();
        tokenRepository = new InMemoryPasswordRecoveryTokenRepository();
        productRepository = new InMemoryProductRepository();
        variantRepository = new InMemoryProductVariantRepository();
        favoriteRepository = new InMemoryFavoriteRepository();
        shoppingListRepository = new InMemoryShoppingListRepository();
        cartRepository = new InMemoryCartRepository();
        orderRepository = new InMemoryOrderRepository();
        paymentRepository = new InMemoryPaymentRepository();
        idempotencyPort = new CountingIdempotencyPort();
        seedCatalogFromDataset();
        initialStock = snapshotStock();
        runner = new LocalHistoricalCustomersDemoRunner(
                userRepository,
                customerRecordRepository,
                addressRepository,
                passwordHasher,
                tokenRepository,
                productRepository,
                variantRepository,
                favoriteRepository,
                shoppingListRepository,
                cartRepository,
                orderRepository,
                paymentRepository,
                idempotencyPort,
                CLOCK,
                true);
    }

    @Test
    void shouldSeedTenCustomersThirtyTwoTerminalOrdersWithoutChangingStock() {
        LocalHistoricalCustomersDemoRunner.SeedResult result = runner.seed();

        assertEquals(10, result.customers());
        assertEquals(32, result.orders());
        assertEquals(32, result.delivered() + result.cancelled());
        assertEquals(10, countDemoCustomers());
        assertEquals(10, countDemoCustomerRecords());
        assertEquals(32, orderRepository.all().size());
        assertEquals(
                0,
                orderRepository.all().stream()
                        .filter(order -> order.status() != OrderStatus.DELIVERED
                                && order.status() != OrderStatus.CANCELLED)
                        .count());
        assertTrue(orderRepository.all().stream()
                .allMatch(order -> order.items().stream()
                        .allMatch(item -> productRepository.findById(item.productId()).isPresent())));
        for (var spec : HistoricalDemoBlueprint.customers()) {
            User user = userRepository.findByEmail(spec.email()).orElseThrow();
            assertTrue(user.customerRecordId() != null);
            assertEquals(
                    customerRecordRepository
                            .findByDocument(HistoricalDemoBlueprint.DOCUMENT_TYPE, spec.documentNumber())
                            .orElseThrow()
                            .id(),
                    user.customerRecordId());
        }
        assertEquals(initialStock, snapshotStock());
        assertEquals(HistoricalDemoBlueprint.PASSWORD, passwordHasher.lastRawPassword());
        assertEquals("SuperF123!", HistoricalDemoBlueprint.PASSWORD);
        CustomerRegistrationRules.requirePassword(HistoricalDemoBlueprint.PASSWORD);
    }

    @Test
    void shouldAuthenticateAllDemoCustomersWithDemoPassword() {
        BCryptPasswordHasher bcrypt = new BCryptPasswordHasher(new BCryptPasswordEncoder());
        LocalHistoricalCustomersDemoRunner bcryptRunner = new LocalHistoricalCustomersDemoRunner(
                userRepository,
                customerRecordRepository,
                addressRepository,
                bcrypt,
                tokenRepository,
                productRepository,
                variantRepository,
                favoriteRepository,
                shoppingListRepository,
                cartRepository,
                orderRepository,
                paymentRepository,
                idempotencyPort,
                CLOCK,
                true);
        bcryptRunner.seed();

        AuthenticateUserUseCase authenticate =
                new AuthenticateUserUseCase(userRepository, bcrypt, new StubAccessTokenIssuer());

        assertEquals(10, HistoricalDemoBlueprint.customers().size());
        for (var customer : HistoricalDemoBlueprint.customers()) {
            AuthenticationResult result = authenticate.execute(
                    new AuthenticateUserCommand(customer.email(), HistoricalDemoBlueprint.PASSWORD));
            assertEquals(Role.CUSTOMER, result.role());
            assertEquals(customer.firstName(), result.firstName());
            assertEquals(customer.lastName(), result.lastName());
            assertTrue(userRepository.findById(result.userId()).isPresent());
        }
    }

    @Test
    void shouldBeIdempotentAndPreserveNonDemoCustomers() {
        User stranger = userRepository.save(User.create(
                UUID.randomUUID(),
                "CC",
                "1999999999",
                "Otro",
                "Cliente",
                "otro.cliente@example.com",
                "3001112233",
                "hashed:x",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                NOW,
                NOW));

        runner.seed();
        assertEquals(10, countDemoCustomers());
        assertEquals(32, orderRepository.all().size());

        runner.seed();
        assertEquals(10, countDemoCustomers());
        assertEquals(32, orderRepository.all().size());
        assertTrue(userRepository.findById(stranger.id()).isPresent());
        assertEquals(initialStock, snapshotStock());
    }

    @Test
    void shouldLinkEachDemoUserToExistingCustomerRecordByDocument() {
        Map<String, UUID> preexistingRecordIds = new LinkedHashMap<>();
        for (var spec : HistoricalDemoBlueprint.customers()) {
            UUID recordId = UUID.randomUUID();
            preexistingRecordIds.put(spec.documentNumber(), recordId);
            customerRecordRepository.save(CustomerRecord.create(
                    recordId,
                    HistoricalDemoBlueprint.DOCUMENT_TYPE,
                    spec.documentNumber(),
                    "Billing",
                    "Preserved",
                    NOW.minusSeconds(3600),
                    NOW.minusSeconds(3600)));
        }

        runner.seed();
        runner.seed();

        assertEquals(10, countDemoCustomerRecords());
        for (var spec : HistoricalDemoBlueprint.customers()) {
            User user = userRepository.findByEmail(spec.email()).orElseThrow();
            assertEquals(UserStatus.ACTIVE, user.status());
            assertEquals(spec.documentNumber(), user.documentNumber());
            assertEquals(spec.phone(), user.phone());
            assertEquals(preexistingRecordIds.get(spec.documentNumber()), user.customerRecordId());

            CustomerRecord record = customerRecordRepository
                    .findByDocument(HistoricalDemoBlueprint.DOCUMENT_TYPE, spec.documentNumber())
                    .orElseThrow();
            assertEquals(preexistingRecordIds.get(spec.documentNumber()), record.id());
            assertEquals("Billing", record.billingFirstName());
            assertEquals("Preserved", record.billingLastName());
        }
    }

    private void seedCatalogFromDataset() {
        Map<String, DatasetProduct> dataset = LocalDemoDataRunner.loadDatasetProducts().stream()
                .collect(java.util.stream.Collectors.toMap(DatasetProduct::id, item -> item, (a, b) -> a));
        for (String code : HistoricalDemoProductIndex.requiredCodes()) {
            DatasetProduct source = dataset.get(code);
            UUID variantId = null;
            if (source.variant() != null && !source.variant().isBlank()) {
                variantId = UUID.nameUUIDFromBytes(("variant-" + code).getBytes());
                variantRepository.save(ProductVariant.create(
                        variantId,
                        TYPE_ID,
                        source.variant(),
                        null,
                        ProductVariantStatus.ACTIVE,
                        NOW,
                        NOW));
            }
            UUID productId = UUID.nameUUIDFromBytes(("product-" + code).getBytes());
            ProductStatus status = ProductStatus.valueOf(source.status());
            productRepository.save(Product.create(
                    productId,
                    CATEGORY_ID,
                    TYPE_ID,
                    variantId,
                    Presentation.of(1, PresentationUnit.UNIT),
                    source.barcode(),
                    source.name(),
                    source.brand(),
                    source.description(),
                    Money.cop(source.price().setScale(2)),
                    source.stock(),
                    source.imageUrl(),
                    status,
                    NOW,
                    NOW));
        }
    }

    private Map<UUID, Integer> snapshotStock() {
        Map<UUID, Integer> stock = new LinkedHashMap<>();
        for (Product product : productRepository.findAll()) {
            stock.put(product.id(), product.stock());
        }
        return Map.copyOf(stock);
    }

    private long countDemoCustomers() {
        return HistoricalDemoBlueprint.customers().stream()
                .filter(spec -> userRepository.findByEmail(spec.email()).isPresent())
                .count();
    }

    private long countDemoCustomerRecords() {
        return HistoricalDemoBlueprint.customers().stream()
                .filter(spec -> customerRecordRepository
                        .findByDocument(HistoricalDemoBlueprint.DOCUMENT_TYPE, spec.documentNumber())
                        .isPresent())
                .count();
    }

    private static final class InMemoryProductRepository implements ProductRepository {
        private final Map<UUID, Product> products = new LinkedHashMap<>();

        @Override
        public Product save(Product product) {
            products.put(product.id(), product);
            return product;
        }

        @Override
        public Optional<Product> findById(UUID id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public List<Product> findByIds(Collection<UUID> ids) {
            return ids.stream().map(products::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public List<Product> findAll() {
            return List.copyOf(products.values());
        }

        @Override
        public List<Product> findByCategoryId(UUID categoryId) {
            return findAll().stream().filter(p -> p.categoryId().equals(categoryId)).toList();
        }

        @Override
        public List<Product> findByStatus(ProductStatus status) {
            return findAll().stream().filter(p -> p.status() == status).toList();
        }

        @Override
        public List<Product> findByCategoryIdAndStatus(UUID categoryId, ProductStatus status) {
            return findAll().stream()
                    .filter(p -> p.categoryId().equals(categoryId) && p.status() == status)
                    .toList();
        }

        @Override
        public List<Product> searchByNameBrandOrBarcode(String text) {
            return List.of();
        }

        @Override
        public boolean adjustStockIfUnchanged(UUID id, int expectedStock, int newStock, Instant updatedAt) {
            throw new UnsupportedOperationException("historical demo must not adjust stock");
        }
    }

    private static final class InMemoryProductVariantRepository implements ProductVariantRepository {
        private final Map<UUID, ProductVariant> variants = new LinkedHashMap<>();

        @Override
        public ProductVariant save(ProductVariant productVariant) {
            variants.put(productVariant.id(), productVariant);
            return productVariant;
        }

        @Override
        public Optional<ProductVariant> findById(UUID id) {
            return Optional.ofNullable(variants.get(id));
        }

        @Override
        public List<ProductVariant> findByIds(Collection<UUID> ids) {
            return ids.stream().map(variants::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public List<ProductVariant> findAll() {
            return List.copyOf(variants.values());
        }

        @Override
        public List<ProductVariant> findByProductTypeId(UUID productTypeId) {
            return findAll().stream().filter(v -> v.productTypeId().equals(productTypeId)).toList();
        }

        @Override
        public List<ProductVariant> findByStatus(ProductVariantStatus status) {
            return findAll().stream().filter(v -> v.status() == status).toList();
        }

        @Override
        public List<ProductVariant> findByProductTypeIdAndStatus(UUID productTypeId, ProductVariantStatus status) {
            return findByProductTypeId(productTypeId).stream().filter(v -> v.status() == status).toList();
        }
    }

    private static final class InMemoryFavoriteRepository implements FavoriteRepositoryPort {
        private final Map<UUID, Favorite> favorites = new ConcurrentHashMap<>();

        @Override
        public Favorite save(Favorite favorite) {
            favorites.put(favorite.id(), favorite);
            return favorite;
        }

        @Override
        public Optional<Favorite> findByCustomerIdAndProductId(UUID customerId, UUID productId) {
            return favorites.values().stream()
                    .filter(f -> f.customerId().equals(customerId) && f.productId().equals(productId))
                    .findFirst();
        }

        @Override
        public List<Favorite> findAllByCustomerId(UUID customerId) {
            return favorites.values().stream().filter(f -> f.customerId().equals(customerId)).toList();
        }

        @Override
        public void deleteByCustomerIdAndProductId(UUID customerId, UUID productId) {
            favorites.values()
                    .removeIf(f -> f.customerId().equals(customerId) && f.productId().equals(productId));
        }

        @Override
        public void deleteAllByCustomerId(UUID customerId) {
            favorites.values().removeIf(f -> f.customerId().equals(customerId));
        }
    }

    private static final class InMemoryShoppingListRepository implements ShoppingListRepositoryPort {
        private final Map<UUID, ShoppingList> lists = new ConcurrentHashMap<>();

        @Override
        public ShoppingList save(ShoppingList shoppingList) {
            lists.put(shoppingList.id(), shoppingList);
            return shoppingList;
        }

        @Override
        public Optional<ShoppingList> findById(UUID shoppingListId) {
            return Optional.ofNullable(lists.get(shoppingListId));
        }

        @Override
        public List<ShoppingList> findAllByCustomerId(UUID customerId) {
            return lists.values().stream().filter(list -> list.customerId().equals(customerId)).toList();
        }

        @Override
        public void delete(UUID shoppingListId) {
            lists.remove(shoppingListId);
        }

        @Override
        public void deleteAllByCustomerId(UUID customerId) {
            lists.values().removeIf(list -> list.customerId().equals(customerId));
        }
    }

    private static final class InMemoryCartRepository implements CartRepositoryPort {
        private final Map<UUID, Cart> byCustomer = new ConcurrentHashMap<>();

        @Override
        public Cart save(Cart cart) {
            byCustomer.put(cart.customerId(), cart);
            return cart;
        }

        @Override
        public Optional<Cart> findByCustomerId(UUID customerId) {
            return Optional.ofNullable(byCustomer.get(customerId));
        }

        @Override
        public void deleteByCustomerId(UUID customerId) {
            byCustomer.remove(customerId);
        }
    }

    private static final class InMemoryOrderRepository implements OrderRepository {
        private final Map<UUID, Order> orders = new LinkedHashMap<>();

        @Override
        public Order save(Order order) {
            orders.put(order.id(), order);
            return order;
        }

        @Override
        public Optional<Order> saveIfCurrent(Order order, OrderStatus fromStatus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Order> findById(UUID orderId) {
            return Optional.ofNullable(orders.get(orderId));
        }

        @Override
        public PagedResult<Order> findByCustomerId(UUID customerId, PageRequest pageRequest) {
            List<Order> items = orders.values().stream()
                    .filter(order -> order.customerId().equals(customerId))
                    .toList();
            return page(items, pageRequest);
        }

        @Override
        public PagedResult<Order> findByCustomerIds(Collection<UUID> customerIds, PageRequest pageRequest) {
            List<Order> items = orders.values().stream()
                    .filter(order -> customerIds.contains(order.customerId()))
                    .toList();
            return page(items, pageRequest);
        }

        @Override
        public PagedResult<Order> findOrdersWithPaymentByCustomerIds(
                Collection<UUID> customerIds, PageRequest pageRequest) {
            List<Order> items = orders.values().stream()
                    .filter(order -> customerIds.contains(order.customerId()) && order.paymentId() != null)
                    .toList();
            return page(items, pageRequest);
        }

        @Override
        public PagedResult<Order> findByAdminFilter(
                com.superfercho.orders.application.dto.AdminOrderFilter filter, PageRequest pageRequest) {
            List<Order> items = orders.values().stream()
                    .filter(order -> !filter.hasStatuses() || filter.statuses().contains(order.status()))
                    .toList();
            return page(items, pageRequest);
        }

        @Override
        public List<Order> findInProgressForLifecycle() {
            return List.of();
        }

        @Override
        public List<Order> findCreatedBetweenExcludingStatus(
                Instant fromInclusive, Instant toExclusive, OrderStatus excludedStatus) {
            return orders.values().stream()
                    .filter(order -> !order.createdAt().isBefore(fromInclusive))
                    .filter(order -> order.createdAt().isBefore(toExclusive))
                    .filter(order -> order.status() != excludedStatus)
                    .sorted(Comparator.comparing(Order::createdAt))
                    .toList();
        }

        @Override
        public void deleteAllByCustomerId(UUID customerId) {
            orders.entrySet().removeIf(entry -> entry.getValue().customerId().equals(customerId));
        }

        @Override
        public List<com.superfercho.orders.application.dto.AdminSalesBucketRow> aggregateSalesBuckets(
                com.superfercho.platform.time.BucketGranularity granularity,
                Instant fromInclusive,
                Instant toExclusive) {
            return List.of();
        }

        @Override
        public com.superfercho.orders.application.dto.AdminBusinessPeriodRow summarizeBusinessPeriod(
                Instant fromInclusive, Instant toExclusive) {
            return new com.superfercho.orders.application.dto.AdminBusinessPeriodRow(0, 0, java.math.BigDecimal.ZERO, 0);
        }

        @Override
        public List<Order> findDeliveredBetween(Instant fromInclusive, Instant toExclusive) {
            return List.of();
        }

        @Override
        public List<com.superfercho.orders.application.dto.AdminProductSalesRow> findTopProductsByQuantity(
                Instant fromInclusive, Instant toExclusive, int limit, boolean ascending) {
            return List.of();
        }

        @Override
        public List<com.superfercho.orders.application.dto.AdminCustomerSalesRow> findTopCustomersByTotal(
                Instant fromInclusive, Instant toExclusive, int limit) {
            return List.of();
        }

        @Override
        public List<com.superfercho.orders.application.dto.AdminCustomerSalesRow> findTopCustomersByOrders(
                Instant fromInclusive, Instant toExclusive, int limit) {
            return List.of();
        }

        List<Order> all() {
            return List.copyOf(orders.values());
        }

        private static PagedResult<Order> page(List<Order> items, PageRequest pageRequest) {
            int from = Math.min(pageRequest.page() * pageRequest.size(), items.size());
            int to = Math.min(from + pageRequest.size(), items.size());
            return new PagedResult<>(items.subList(from, to), pageRequest.page(), pageRequest.size(), items.size());
        }
    }

    private static final class InMemoryPaymentRepository implements PaymentRepository {
        private final Map<UUID, Payment> payments = new ConcurrentHashMap<>();

        @Override
        public Payment save(Payment payment) {
            payments.put(payment.id(), payment);
            return payment;
        }

        @Override
        public Optional<Payment> findById(UUID paymentId) {
            return Optional.ofNullable(payments.get(paymentId));
        }

        @Override
        public void delete(UUID paymentId) {
            payments.remove(paymentId);
        }
    }

    private static final class CountingIdempotencyPort implements IdempotencyPort {
        private final AtomicInteger deletions = new AtomicInteger();

        @Override
        public Optional<com.superfercho.orders.application.dto.IdempotencyRecord> find(
                String key, UUID customerId) {
            return Optional.empty();
        }

        @Override
        public void save(com.superfercho.orders.application.dto.IdempotencyRecord record) {}

        @Override
        public void deleteAllByCustomerId(UUID customerId) {
            deletions.incrementAndGet();
        }
    }

    private static final class StubAccessTokenIssuer implements AccessTokenIssuer {
        @Override
        public IssuedAccessToken issue(UUID userId, Role role) {
            return issue(userId, role, null);
        }

        @Override
        public IssuedAccessToken issue(UUID userId, Role role, UUID previewId) {
            return new IssuedAccessToken("token-" + userId, Instant.EPOCH.plusSeconds(3600));
        }
    }
}
