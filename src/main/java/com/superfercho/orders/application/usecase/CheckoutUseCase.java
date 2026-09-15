package com.superfercho.orders.application.usecase;

import com.superfercho.catalog.application.dto.StockDecrementResult;
import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.catalog.application.dto.UnavailableProduct;
import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.orders.application.dto.AddressSnapshot;
import com.superfercho.orders.application.dto.AvailabilityResult;
import com.superfercho.orders.application.dto.CartItemSnapshot;
import com.superfercho.orders.application.dto.CartSnapshot;
import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutItem;
import com.superfercho.orders.application.dto.CheckoutRequestFingerprint;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.IdempotencyRecord;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentRequest;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.dto.ProductCatalogInfo;
import com.superfercho.orders.application.exception.AddressNotAvailableException;
import com.superfercho.orders.application.exception.CartEmptyException;
import com.superfercho.orders.application.exception.InvalidCheckoutException;
import com.superfercho.orders.application.exception.PaymentDeclinedException;
import com.superfercho.orders.application.exception.ProductNotAvailableException;
import com.superfercho.orders.application.exception.ProductPriceChangedException;
import com.superfercho.orders.application.exception.StockUnavailableException;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.CurrentUserProvider;
import com.superfercho.orders.application.port.CustomerAddressPort;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.application.port.ProductCatalogPort;
import com.superfercho.orders.application.port.ShoppingCartPort;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application transaction boundary for checkout. Infrastructure must wrap
 * {@link #execute(CheckoutCommand)} in a single local database transaction so
 * payment, inventory, order persistence, cart clear, and idempotency commit or
 * roll back together. This class does not start a transaction.
 */
public final class CheckoutUseCase {

    static final Duration IDEMPOTENCY_RETENTION = Duration.ofHours(24);

    private final CurrentUserProvider currentUserProvider;
    private final ClockProvider clockProvider;
    private final ShoppingCartPort shoppingCartPort;
    private final CustomerAddressPort customerAddressPort;
    private final ProductCatalogPort productCatalogPort;
    private final InventoryPort inventoryPort;
    private final PaymentPort paymentPort;
    private final OrderRepository orderRepository;
    private final IdempotencyPort idempotencyPort;

    public CheckoutUseCase(
            CurrentUserProvider currentUserProvider,
            ClockProvider clockProvider,
            ShoppingCartPort shoppingCartPort,
            CustomerAddressPort customerAddressPort,
            ProductCatalogPort productCatalogPort,
            InventoryPort inventoryPort,
            PaymentPort paymentPort,
            OrderRepository orderRepository,
            IdempotencyPort idempotencyPort) {
        this.currentUserProvider = currentUserProvider;
        this.clockProvider = clockProvider;
        this.shoppingCartPort = shoppingCartPort;
        this.customerAddressPort = customerAddressPort;
        this.productCatalogPort = productCatalogPort;
        this.inventoryPort = inventoryPort;
        this.paymentPort = paymentPort;
        this.orderRepository = orderRepository;
        this.idempotencyPort = idempotencyPort;
    }

    public CheckoutResult execute(CheckoutCommand command) {
        UUID customerId = currentUserProvider.getCurrentUserId();
        Instant now = clockProvider.currentTime();
        requireIdempotencyKey(command);
        CheckoutRequestFingerprint fingerprint = CheckoutRequestFingerprint.from(command);
        CheckoutResult replayed =
                idempotencyPort.validateReuse(command.idempotencyKey(), customerId, fingerprint, now).orElse(null);
        if (replayed != null) {
            return replayed;
        }
        CartSnapshot cart = shoppingCartPort.getActiveCart(customerId);
        if (cart.isEmpty()) {
            throw new CartEmptyException();
        }

        AddressSnapshot address = customerAddressPort
                .getAddressForCustomer(customerId, command.addressId())
                .orElseThrow(() -> new AddressNotAvailableException(command.addressId()));

        List<OrderItem> items = buildOrderItems(command, cart);
        AvailabilityResult availability = productCatalogPort.checkAvailability(stockQuantities(cart));
        if (!availability.allAvailable()) {
            throw new StockUnavailableException(unavailable(availability, cart));
        }

        UUID orderId = UUID.randomUUID();
        Order draft = Order.create(
                orderId,
                orderNumberFor(orderId),
                customerId,
                items,
                toShippingAddress(address),
                null,
                now,
                now);

        PaymentResult payment = paymentPort.processPayment(
                new PaymentRequest(orderId, draft.total(), command.paymentMethod()));
        if (!isPaymentAcceptable(command.paymentMethod(), payment.status())) {
            throw new PaymentDeclinedException();
        }

        StockDecrementResult stock = inventoryPort.decreaseStockAtomically(stockQuantities(cart));
        if (!stock.succeeded()) {
            throw new StockUnavailableException(stock.unavailableProducts());
        }

        Order order = Order.create(
                orderId,
                draft.orderNumber(),
                customerId,
                items,
                draft.shippingAddress(),
                payment.paymentId(),
                now,
                now);
        Order saved = orderRepository.save(order);
        shoppingCartPort.clearCart(customerId);

        CheckoutResult result = new CheckoutResult(
                saved.id(),
                saved.orderNumber().value(),
                saved.status(),
                payment.status(),
                saved.total());
        idempotencyPort.save(new IdempotencyRecord(
                command.idempotencyKey(),
                customerId,
                fingerprint,
                result,
                now,
                now.plus(IDEMPOTENCY_RETENTION)));
        return result;
    }

    private List<OrderItem> buildOrderItems(CheckoutCommand command, CartSnapshot cart) {
        Map<UUID, CartItemSnapshot> cartByProduct = new LinkedHashMap<>();
        for (CartItemSnapshot cartItem : cart.items()) {
            cartByProduct.put(cartItem.productId(), cartItem);
        }

        List<OrderItem> items = new ArrayList<>();
        for (CheckoutItem line : command.items()) {
            CartItemSnapshot cartItem = cartByProduct.get(line.productId());
            if (cartItem == null) {
                throw new InvalidCheckoutException("product is not in the active cart: " + line.productId());
            }
            if (cartItem.quantity() != line.quantity()) {
                throw new InvalidCheckoutException(
                        "quantity does not match the active cart for product: " + line.productId());
            }
        }
        if (cartByProduct.size() != command.items().size()) {
            throw new InvalidCheckoutException("checkout items must match the active cart");
        }

        for (CheckoutItem line : command.items()) {
            ProductCatalogInfo product = productCatalogPort
                    .getProduct(line.productId())
                    .orElseThrow(() -> new ProductNotAvailableException(line.productId()));
            if (!product.active() || !product.available()) {
                throw new ProductNotAvailableException(line.productId());
            }
            if (!line.expectedUnitPrice().equals(product.currentPrice())) {
                throw new ProductPriceChangedException(
                        line.productId(), line.expectedUnitPrice(), product.currentPrice());
            }
            items.add(OrderItem.create(
                    UUID.randomUUID(),
                    product.productId(),
                    product.name(),
                    product.currentPrice(),
                    line.quantity()));
        }
        return List.copyOf(items);
    }

    private static List<StockQuantity> stockQuantities(CartSnapshot cart) {
        return cart.items().stream()
                .map(item -> new StockQuantity(item.productId(), item.quantity()))
                .toList();
    }

    private static List<UnavailableProduct> unavailable(AvailabilityResult availability, CartSnapshot cart) {
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (CartItemSnapshot item : cart.items()) {
            quantities.put(item.productId(), item.quantity());
        }
        return availability.unavailableProductIds().stream()
                .map(productId -> new UnavailableProduct(productId, quantities.getOrDefault(productId, 0)))
                .toList();
    }

    private static ShippingAddressSnapshot toShippingAddress(AddressSnapshot address) {
        return new ShippingAddressSnapshot(
                address.recipientName(),
                address.addressLine(),
                address.additionalInfo(),
                address.city(),
                address.department(),
                address.phone());
    }

    private static OrderNumber orderNumberFor(UUID orderId) {
        return new OrderNumber("ORD-" + orderId.toString().replace("-", "").substring(0, 12).toUpperCase());
    }

    private static boolean isPaymentAcceptable(PaymentMethod method, PaymentStatus status) {
        if (status == PaymentStatus.DECLINED) {
            return false;
        }
        return switch (method) {
            case SIMULATED_CARD -> status == PaymentStatus.APPROVED;
            case CASH_ON_DELIVERY -> status == PaymentStatus.PENDING;
        };
    }

    private static void requireIdempotencyKey(CheckoutCommand command) {
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new InvalidCheckoutException("idempotencyKey cannot be null or blank");
        }
        if (command.paymentMethod() == null) {
            throw new InvalidCheckoutException("paymentMethod cannot be null");
        }
        if (command.addressId() == null) {
            throw new InvalidCheckoutException("addressId cannot be null");
        }
    }
}
