package com.superfercho.platform.demo.historical;

import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.validation.CustomerRegistrationRules;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.payments.application.port.PaymentRepository;
import com.superfercho.payments.domain.model.Payment;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
import com.superfercho.platform.demo.historical.HistoricalDemoBlueprint.AddressSpec;
import com.superfercho.platform.demo.historical.HistoricalDemoBlueprint.CustomerSpec;
import com.superfercho.platform.demo.historical.HistoricalDemoBlueprint.OrderOutcome;
import com.superfercho.platform.demo.historical.HistoricalDemoBlueprint.OrderSpec;
import com.superfercho.platform.demo.historical.HistoricalDemoBlueprint.ShoppingListSpec;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartItem;
import com.superfercho.shopping.domain.model.CartStatus;
import com.superfercho.shopping.domain.model.Favorite;
import com.superfercho.shopping.domain.model.ShoppingList;
import com.superfercho.shopping.domain.model.ShoppingListItem;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local/demo seed of 10 historical customers with shopping data and terminal orders.
 *
 * <p>Physical cleanup + recreate (idempotent). Does not touch Product.stock, checkout,
 * lifecycle jobs, cancel/refund use cases, or the catalog demo runner.
 */
@Component
@Profile("local")
@org.springframework.core.annotation.Order(300)
public class LocalHistoricalCustomersDemoRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalHistoricalCustomersDemoRunner.class);
    private static final Duration ORDER_SPACING = Duration.ofDays(3);
    private static final Duration DELIVERED_LAG = Duration.ofMinutes(6);
    private static final Duration CANCELLED_LAG = Duration.ofMinutes(1);

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final FavoriteRepositoryPort favoriteRepository;
    private final ShoppingListRepositoryPort shoppingListRepository;
    private final CartRepositoryPort cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final IdempotencyPort idempotencyPort;
    private final Clock clock;
    private final boolean enabled;

    public LocalHistoricalCustomersDemoRunner(
            UserRepository userRepository,
            AddressRepository addressRepository,
            PasswordHasher passwordHasher,
            PasswordRecoveryTokenRepository passwordRecoveryTokenRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            FavoriteRepositoryPort favoriteRepository,
            ShoppingListRepositoryPort shoppingListRepository,
            CartRepositoryPort cartRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            IdempotencyPort idempotencyPort,
            Clock clock,
            @Value("${superfercho.dev.demo-seed.enabled:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordHasher = passwordHasher;
        this.passwordRecoveryTokenRepository = passwordRecoveryTokenRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.favoriteRepository = favoriteRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.idempotencyPort = idempotencyPort;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            LOGGER.info("Historical customers demo seed skipped: superfercho.dev.demo-seed.enabled=false");
            return;
        }
        // Guard against shell-leaked SPRING_PROFILES_ACTIVE=local poisoning @SpringBootTest contexts.
        if (isAutomatedTestRuntime()) {
            LOGGER.info("Historical customers demo seed skipped: automated test runtime detected");
            return;
        }
        SeedResult result = seed();
        LOGGER.info(
                "Historical customers demo seeded ({} customers, {} orders: {} delivered, {} cancelled). Stock unchanged for {} indexed products.",
                result.customers(),
                result.orders(),
                result.delivered(),
                result.cancelled(),
                result.stockCheckedProducts());
    }

    private static boolean isAutomatedTestRuntime() {
        String command = System.getProperty("sun.java.command", "");
        if (command.contains("surefire") || command.contains("failsafe")) {
            return true;
        }
        return StackWalker.getInstance().walk(frames -> frames.anyMatch(frame -> {
            String className = frame.getClassName();
            return className.startsWith("org.junit.")
                    || className.startsWith("org.springframework.test.")
                    || className.contains("surefire")
                    || className.contains("failsafe");
        }));
    }

    SeedResult seed() {
        if (productRepository.findAll().isEmpty()) {
            throw new IllegalStateException(
                    "Historical customers demo requires the local catalog seed to run first");
        }
        HistoricalDemoProductIndex catalog =
                HistoricalDemoProductIndex.build(productRepository, productVariantRepository);
        Map<UUID, Integer> stockBefore = catalog.snapshotStock();

        cleanupDemoCustomers();
        Instant now = clock.instant();
        CustomerRegistrationRules.requirePassword(HistoricalDemoBlueprint.PASSWORD);
        String passwordHash = passwordHasher.hash(HistoricalDemoBlueprint.PASSWORD);

        int orderCount = 0;
        int delivered = 0;
        int cancelled = 0;
        for (CustomerSpec spec : HistoricalDemoBlueprint.customers()) {
            User user = createCustomer(spec, passwordHash, now);
            Address defaultAddress = createAddresses(user, spec, now);
            createFavorites(user.id(), spec.favoriteProductCodes(), catalog, now);
            createShoppingLists(user.id(), spec.shoppingLists(), catalog, now);
            createCart(user.id(), spec.cartProductCodes(), catalog, now);
            OrderCounts counts = createOrders(user, defaultAddress, spec.orders(), catalog, now);
            orderCount += counts.total();
            delivered += counts.delivered();
            cancelled += counts.cancelled();
        }

        Map<UUID, Integer> stockAfter = reloadStock(stockBefore.keySet());
        assertStockUnchanged(stockBefore, stockAfter);
        if (HistoricalDemoBlueprint.customers().size() != HistoricalDemoBlueprint.EXPECTED_CUSTOMERS) {
            throw new IllegalStateException("Blueprint customer count mismatch");
        }
        if (orderCount != HistoricalDemoBlueprint.EXPECTED_ORDERS) {
            throw new IllegalStateException(
                    "Expected " + HistoricalDemoBlueprint.EXPECTED_ORDERS + " orders, created " + orderCount);
        }
        return new SeedResult(
                HistoricalDemoBlueprint.EXPECTED_CUSTOMERS,
                orderCount,
                delivered,
                cancelled,
                stockBefore.size());
    }

    void cleanupDemoCustomers() {
        Set<UUID> cleaned = new LinkedHashSet<>();
        for (CustomerSpec spec : HistoricalDemoBlueprint.customers()) {
            Optional<User> byEmail = userRepository.findByEmail(User.normalizeEmail(spec.email()));
            Optional<User> byDocument =
                    userRepository.findByDocument(HistoricalDemoBlueprint.DOCUMENT_TYPE, spec.documentNumber());
            byEmail.ifPresent(user -> cleaned.add(user.id()));
            byDocument.ifPresent(user -> {
                if (user.role() != Role.CUSTOMER) {
                    throw new IllegalStateException(
                            "Refusing to delete non-CUSTOMER matched by demo document " + spec.documentNumber());
                }
                cleaned.add(user.id());
            });
        }
        for (UUID userId : cleaned) {
            deleteCustomerGraph(userId);
        }
    }

    private void deleteCustomerGraph(UUID customerId) {
        List<Order> orders = loadAllOrders(customerId);
        Set<UUID> paymentIds = new HashSet<>();
        for (Order order : orders) {
            if (order.paymentId() != null) {
                paymentIds.add(order.paymentId());
            }
        }
        for (UUID paymentId : paymentIds) {
            paymentRepository.delete(paymentId);
        }
        orderRepository.deleteAllByCustomerId(customerId);
        idempotencyPort.deleteAllByCustomerId(customerId);
        cartRepository.deleteByCustomerId(customerId);
        favoriteRepository.deleteAllByCustomerId(customerId);
        shoppingListRepository.deleteAllByCustomerId(customerId);
        passwordRecoveryTokenRepository.deleteAllByUserId(customerId);
        addressRepository.deleteAllByUserId(customerId);
        userRepository.deleteById(customerId);
    }

    private User createCustomer(CustomerSpec spec, String passwordHash, Instant now) {
        User user = User.create(
                UUID.randomUUID(),
                HistoricalDemoBlueprint.DOCUMENT_TYPE,
                spec.documentNumber(),
                spec.firstName(),
                spec.lastName(),
                spec.email(),
                spec.phone(),
                passwordHash,
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                now,
                now);
        return userRepository.save(user);
    }

    private Address createAddresses(User user, CustomerSpec spec, Instant now) {
        Address defaultAddress = null;
        for (AddressSpec addressSpec : spec.addresses()) {
            Address saved = addressRepository.save(
                    user.id(),
                    Address.create(
                            UUID.randomUUID(),
                            addressSpec.label(),
                            user.displayFullName(),
                            addressSpec.addressLine(),
                            addressSpec.additionalInfo(),
                            addressSpec.city(),
                            addressSpec.department(),
                            addressSpec.phone(),
                            addressSpec.isDefault(),
                            AddressStatus.ACTIVE,
                            now,
                            now));
            if (addressSpec.isDefault()) {
                defaultAddress = saved;
            }
        }
        if (defaultAddress == null) {
            throw new IllegalStateException("Customer " + spec.code() + " must have a default address");
        }
        return defaultAddress;
    }

    private void createFavorites(
            UUID customerId, List<String> codes, HistoricalDemoProductIndex catalog, Instant now) {
        for (String code : codes) {
            Product product = catalog.require(code);
            favoriteRepository.save(Favorite.create(UUID.randomUUID(), customerId, product.id(), now));
        }
    }

    private void createShoppingLists(
            UUID customerId,
            List<ShoppingListSpec> lists,
            HistoricalDemoProductIndex catalog,
            Instant now) {
        for (ShoppingListSpec listSpec : lists) {
            List<ShoppingListItem> items = new ArrayList<>();
            for (String code : listSpec.productCodes()) {
                items.add(ShoppingListItem.create(UUID.randomUUID(), catalog.require(code).id(), 1, now));
            }
            shoppingListRepository.save(ShoppingList.create(
                    UUID.randomUUID(), customerId, listSpec.name(), items, now, now));
        }
    }

    private void createCart(
            UUID customerId, List<String> codes, HistoricalDemoProductIndex catalog, Instant now) {
        List<CartItem> items = new ArrayList<>();
        for (String code : codes) {
            Product product = catalog.require(code);
            items.add(CartItem.create(
                    UUID.randomUUID(), product.id(), 1, product.price(), now, now));
        }
        cartRepository.save(Cart.create(
                UUID.randomUUID(), customerId, CartStatus.ACTIVE, items, now, now));
    }

    private OrderCounts createOrders(
            User user,
            Address shippingAddress,
            List<OrderSpec> orders,
            HistoricalDemoProductIndex catalog,
            Instant now) {
        int delivered = 0;
        int cancelled = 0;
        Instant cursor = now.minus(ORDER_SPACING.multipliedBy(orders.size() + 1L));
        int index = 1;
        for (OrderSpec orderSpec : orders) {
            Instant createdAt = cursor;
            cursor = cursor.plus(ORDER_SPACING);
            if (orderSpec.outcome() == OrderOutcome.DELIVERED) {
                createDeliveredOrder(user, shippingAddress, orderSpec, catalog, createdAt, index++);
                delivered++;
            } else {
                createCancelledOrder(user, shippingAddress, orderSpec, catalog, createdAt, index++);
                cancelled++;
            }
        }
        return new OrderCounts(delivered + cancelled, delivered, cancelled);
    }

    private void createDeliveredOrder(
            User user,
            Address shippingAddress,
            OrderSpec orderSpec,
            HistoricalDemoProductIndex catalog,
            Instant createdAt,
            int index) {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        List<OrderItem> items = orderItems(orderSpec.productCodes(), catalog);
        Money total = sum(items);
        Instant updatedAt = createdAt.plus(DELIVERED_LAG);
        boolean card = index % 2 == 1;
        Payment payment = card
                ? Payment.create(
                        paymentId,
                        orderId,
                        total,
                        PaymentMethod.SIMULATED_CARD,
                        PaymentStatus.APPROVED,
                        "demo-sim-" + shortId(orderId),
                        createdAt)
                : Payment.create(
                        paymentId,
                        orderId,
                        total,
                        PaymentMethod.CASH_ON_DELIVERY,
                        PaymentStatus.PENDING,
                        "demo-cod-" + shortId(orderId),
                        createdAt);
        paymentRepository.save(payment);
        Order order = Order.reconstitute(
                orderId,
                orderNumber(user, index),
                user.id(),
                OrderStatus.DELIVERED,
                items,
                shippingSnapshot(user, shippingAddress),
                paymentId,
                createdAt,
                createdAt,
                null,
                updatedAt);
        orderRepository.save(order);
    }

    private void createCancelledOrder(
            User user,
            Address shippingAddress,
            OrderSpec orderSpec,
            HistoricalDemoProductIndex catalog,
            Instant createdAt,
            int index) {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        List<OrderItem> items = orderItems(orderSpec.productCodes(), catalog);
        Money total = sum(items);
        Instant cancelledAt = createdAt.plus(CANCELLED_LAG);
        boolean card = index % 2 == 0;
        Payment payment;
        if (card) {
            payment = Payment.reconstitute(
                    paymentId,
                    orderId,
                    total,
                    PaymentMethod.SIMULATED_CARD,
                    PaymentStatus.APPROVED,
                    "demo-sim-refund-" + shortId(orderId),
                    createdAt,
                    cancelledAt,
                    cancelledAt);
        } else {
            payment = Payment.create(
                    paymentId,
                    orderId,
                    total,
                    PaymentMethod.CASH_ON_DELIVERY,
                    PaymentStatus.PENDING,
                    "demo-cod-cancel-" + shortId(orderId),
                    createdAt);
        }
        paymentRepository.save(payment);
        Order order = Order.reconstitute(
                orderId,
                orderNumber(user, index),
                user.id(),
                OrderStatus.CANCELLED,
                items,
                shippingSnapshot(user, shippingAddress),
                paymentId,
                createdAt,
                null,
                cancelledAt,
                cancelledAt);
        orderRepository.save(order);
    }

    private List<OrderItem> orderItems(List<String> codes, HistoricalDemoProductIndex catalog) {
        List<OrderItem> items = new ArrayList<>();
        for (String code : codes) {
            Product product = catalog.require(code);
            int quantity = plausibleQuantity(product);
            items.add(OrderItem.create(
                    UUID.randomUUID(), product.id(), product.name(), product.price(), quantity));
        }
        return List.copyOf(items);
    }

    private static int plausibleQuantity(Product product) {
        String name = product.name().toLowerCase();
        if (name.contains("arroz") || name.contains("aceite") || name.contains("detergente")) {
            return 1;
        }
        if (name.contains("huevos") || name.contains("agua") || name.contains("gaseosa")) {
            return 2;
        }
        return 1;
    }

    private static Money sum(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce((left, right) -> new Money(left.amount().add(right.amount()), Money.COP))
                .orElse(Money.cop(java.math.BigDecimal.ZERO.setScale(2)));
    }

    private static ShippingAddressSnapshot shippingSnapshot(User user, Address address) {
        return new ShippingAddressSnapshot(
                user.displayFullName(),
                address.addressLine(),
                address.additionalInfo(),
                address.city(),
                address.department(),
                address.phone());
    }

    private static OrderNumber orderNumber(User user, int index) {
        String token = user.id().toString().replace("-", "").substring(0, 8).toUpperCase();
        return new OrderNumber("ORD-H-" + token + "-" + String.format("%02d", index));
    }

    private static String shortId(UUID id) {
        return id.toString().replace("-", "").substring(0, 8);
    }

    private List<Order> loadAllOrders(UUID customerId) {
        List<Order> orders = new ArrayList<>();
        int page = 0;
        while (true) {
            PagedResult<Order> result =
                    orderRepository.findByCustomerId(customerId, new PageRequest(page, PageRequest.MAX_SIZE));
            orders.addAll(result.items());
            long loaded = (long) page * PageRequest.MAX_SIZE + result.items().size();
            if (result.items().isEmpty() || loaded >= result.totalElements()) {
                break;
            }
            page++;
        }
        return orders;
    }

    private Map<UUID, Integer> reloadStock(Set<UUID> productIds) {
        Map<UUID, Integer> stock = new HashMap<>();
        for (Product product : productRepository.findByIds(productIds)) {
            stock.put(product.id(), product.stock());
        }
        if (stock.size() != productIds.size()) {
            throw new IllegalStateException("Stock verification could not reload every indexed product");
        }
        return stock;
    }

    private static void assertStockUnchanged(Map<UUID, Integer> before, Map<UUID, Integer> after) {
        if (!before.equals(after)) {
            throw new IllegalStateException(
                    "Historical demo seed mutated product stock; before=" + before + " after=" + after);
        }
    }

    record SeedResult(int customers, int orders, int delivered, int cancelled, int stockCheckedProducts) {}

    private record OrderCounts(int total, int delivered, int cancelled) {}
}
