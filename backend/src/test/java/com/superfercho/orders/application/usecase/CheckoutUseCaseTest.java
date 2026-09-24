package com.superfercho.orders.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.superfercho.orders.application.exception.IdempotencyConflictException;
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
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADDRESS_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CART_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PAYMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));
    private static final int QUANTITY = 2;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ClockProvider clockProvider;

    @Mock
    private ShoppingCartPort shoppingCartPort;

    @Mock
    private CustomerAddressPort customerAddressPort;

    @Mock
    private ProductCatalogPort productCatalogPort;

    @Mock
    private InventoryPort inventoryPort;

    @Mock
    private PaymentPort paymentPort;

    @Mock
    private OrderRepository orderRepository;

    private InMemoryIdempotencyPort idempotencyPort;
    private CheckoutUseCase checkout;

    @BeforeEach
    void setUp() {
        idempotencyPort = new InMemoryIdempotencyPort();
        checkout = new CheckoutUseCase(
                currentUserProvider,
                clockProvider,
                shoppingCartPort,
                customerAddressPort,
                productCatalogPort,
                inventoryPort,
                paymentPort,
                orderRepository,
                idempotencyPort);
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(clockProvider.currentTime()).thenReturn(NOW);
    }

    @Test
    void shouldCompleteCheckoutWhenSimulatedCardIsApproved() {
        givenReadyCartAndCatalog();
        givenApprovedCardPayment();

        CheckoutResult result = checkout.execute(cardCommand());

        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(PaymentStatus.APPROVED, result.paymentStatus());
        assertEquals(Money.cop(new BigDecimal("21.00")), result.total());
        verify(shoppingCartPort).clearCart(CUSTOMER_ID);
        verify(inventoryPort).decreaseStockAtomically(List.of(new StockQuantity(PRODUCT_ID, QUANTITY)));
    }

    @Test
    void shouldCompleteCheckoutWhenCashOnDeliveryIsPending() {
        givenReadyCartAndCatalog();
        givenPendingCashPayment();

        CheckoutResult result = checkout.execute(codCommand());

        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(PaymentStatus.PENDING, result.paymentStatus());
        verify(shoppingCartPort).clearCart(CUSTOMER_ID);
        verify(paymentPort, never()).refundPayment(any());
    }

    @Test
    void shouldRejectEmptyCart() {
        when(shoppingCartPort.getActiveCart(CUSTOMER_ID)).thenReturn(new CartSnapshot(CART_ID, List.of()));

        assertThrows(CartEmptyException.class, () -> checkout.execute(cardCommand()));
        verifyNoSideEffects();
    }

    @Test
    void shouldRejectMissingProduct() {
        givenReadyCartAndCatalog();
        when(productCatalogPort.getProduct(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotAvailableException.class, () -> checkout.execute(cardCommand()));
        verifyNoSideEffects();
    }

    @Test
    void shouldRejectInactiveProduct() {
        givenReadyCartAndCatalog();
        when(productCatalogPort.getProduct(PRODUCT_ID))
                .thenReturn(Optional.of(product(false, true)));

        assertThrows(ProductNotAvailableException.class, () -> checkout.execute(cardCommand()));
        verifyNoSideEffects();
    }

    @Test
    void shouldRejectInsufficientStock() {
        givenReadyCartAndCatalog();
        when(productCatalogPort.checkAvailability(any()))
                .thenReturn(AvailabilityResult.unavailable(List.of(PRODUCT_ID)));

        StockUnavailableException error =
                assertThrows(StockUnavailableException.class, () -> checkout.execute(cardCommand()));

        assertEquals(PRODUCT_ID, error.unavailableProducts().get(0).productId());
        verifyNoSideEffects();
    }

    @Test
    void shouldRejectChangedPriceWithoutAcceptance() {
        givenReadyCartAndCatalog();
        when(productCatalogPort.getProduct(PRODUCT_ID))
                .thenReturn(Optional.of(new ProductCatalogInfo(
                        PRODUCT_ID, "Leche entera", Money.cop(new BigDecimal("12.00")), true, true)));

        assertThrows(ProductPriceChangedException.class, () -> checkout.execute(cardCommand()));
        verifyNoSideEffects();
    }

    @Test
    void shouldRejectAddressNotBelongingToCustomer() {
        givenReadyCartAndCatalog();
        when(customerAddressPort.getAddressForCustomer(CUSTOMER_ID, ADDRESS_ID)).thenReturn(Optional.empty());

        assertThrows(AddressNotAvailableException.class, () -> checkout.execute(cardCommand()));
        verifyNoSideEffects();
    }

    @Test
    void shouldNotCompleteOrderWhenPaymentIsDeclined() {
        givenReadyCartAndCatalog();
        when(paymentPort.processPayment(any()))
                .thenReturn(paymentResult(PaymentStatus.DECLINED, "sim-declined"));

        assertThrows(PaymentDeclinedException.class, () -> checkout.execute(cardCommand()));
        verify(inventoryPort, never()).decreaseStockAtomically(any());
        verify(orderRepository, never()).save(any());
        verify(shoppingCartPort, never()).clearCart(any());
    }

    @Test
    void shouldFailConsistentlyWhenInventoryDecrementFailsAfterApprovedPayment() {
        givenReadyCartAndCatalog();
        givenApprovedCardPayment();
        when(inventoryPort.decreaseStockAtomically(any()))
                .thenReturn(StockDecrementResult.unavailable(List.of(new UnavailableProduct(PRODUCT_ID, QUANTITY))));

        assertThrows(StockUnavailableException.class, () -> checkout.execute(cardCommand()));
        verify(orderRepository, never()).save(any());
        verify(shoppingCartPort, never()).clearCart(any());
    }

    @Test
    void shouldIdentifyCustomerFromCurrentUserProviderNeverFromCommand() {
        givenReadyCartAndCatalog();
        givenApprovedCardPayment();

        checkout.execute(cardCommand());

        assertFalse(Arrays.stream(CheckoutCommand.class.getRecordComponents())
                .anyMatch(component -> "customerId".equals(component.getName())));
        verify(shoppingCartPort).getActiveCart(CUSTOMER_ID);
        verify(customerAddressPort).getAddressForCustomer(CUSTOMER_ID, ADDRESS_ID);
        verify(shoppingCartPort).clearCart(CUSTOMER_ID);
    }

    @Test
    void shouldSaveIdempotencyRecordForNewKey() {
        givenReadyCartAndCatalog();
        givenApprovedCardPayment();

        CheckoutResult result = checkout.execute(cardCommand());

        IdempotencyRecord saved = idempotencyPort.find("key-1", CUSTOMER_ID).orElseThrow();
        assertEquals(result, saved.result());
        assertEquals(CheckoutRequestFingerprint.from(cardCommand()), saved.fingerprint());
        assertEquals(NOW.plus(CheckoutUseCase.IDEMPOTENCY_RETENTION), saved.expiresAt());
    }

    @Test
    void shouldReplaySameIdempotencyKeyAndSameRequest() {
        givenReadyCartAndCatalog();
        givenApprovedCardPayment();
        CheckoutResult first = checkout.execute(cardCommand());

        CheckoutResult second = checkout.execute(cardCommand());

        assertEquals(first, second);
        verify(paymentPort, times(1)).processPayment(any());
        verify(orderRepository, times(1)).save(any());
    }

    @Test
    void shouldConflictWhenSameIdempotencyKeyHasDifferentRequest() {
        givenReadyCartAndCatalog();
        givenApprovedCardPayment();
        checkout.execute(cardCommand());

        CheckoutCommand differentAddress = new CheckoutCommand(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_ID, QUANTITY, PRICE)),
                "key-1");

        assertThrows(IdempotencyConflictException.class, () -> checkout.execute(differentAddress));
        verify(paymentPort, times(1)).processPayment(any());
    }

    @Test
    void shouldConflictWhenSameIdempotencyKeyHasDifferentQuantity() {
        givenReadyCartAndCatalog();
        givenApprovedCardPayment();
        checkout.execute(cardCommand());

        CheckoutCommand differentQuantity = new CheckoutCommand(
                ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_ID, 3, PRICE)),
                "key-1");

        assertThrows(IdempotencyConflictException.class, () -> checkout.execute(differentQuantity));
        verify(paymentPort, times(1)).processPayment(any());
    }

    @Test
    void shouldReplayIdenticalRetryAfterCartIsCleared() {
        givenReadyCartAndCatalog();
        givenApprovedCardPayment();
        CheckoutResult first = checkout.execute(cardCommand());
        lenient()
                .when(shoppingCartPort.getActiveCart(CUSTOMER_ID))
                .thenReturn(new CartSnapshot(CART_ID, List.of()));

        CheckoutResult second = checkout.execute(cardCommand());

        assertEquals(first, second);
        verify(paymentPort, times(1)).processPayment(any());
        verify(orderRepository, times(1)).save(any());
        verify(shoppingCartPort, times(1)).getActiveCart(CUSTOMER_ID);
        verify(shoppingCartPort, times(1)).clearCart(CUSTOMER_ID);
    }

    @Test
    void shouldRejectCommandProductMissingFromCart() {
        givenReadyCartAndCatalog();
        UUID unknownProduct = UUID.fromString("88888888-8888-8888-8888-888888888888");
        CheckoutCommand unknown = new CheckoutCommand(
                ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(unknownProduct, QUANTITY, PRICE)),
                "key-1");

        assertThrows(InvalidCheckoutException.class, () -> checkout.execute(unknown));
        verifyNoSideEffects();
    }

    @Test
    void shouldRejectCommandQuantityDifferentFromCart() {
        givenReadyCartAndCatalog();
        CheckoutCommand differentQuantity = new CheckoutCommand(
                ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_ID, 3, PRICE)),
                "key-1");

        assertThrows(InvalidCheckoutException.class, () -> checkout.execute(differentQuantity));
        verifyNoSideEffects();
    }

    @Test
    void shouldPassCartQuantitiesToInventoryPort() {
        givenReadyCartAndCatalog();
        givenApprovedCardPayment();

        checkout.execute(cardCommand());

        verify(inventoryPort).decreaseStockAtomically(List.of(new StockQuantity(PRODUCT_ID, QUANTITY)));
        ArgumentCaptor<PaymentRequest> payment = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentPort).processPayment(payment.capture());
        assertEquals(Money.cop(new BigDecimal("21.00")), payment.getValue().amount());
        assertEquals(PaymentMethod.SIMULATED_CARD, payment.getValue().paymentMethod());
    }

    @Test
    void shouldPersistOrderWithCatalogAndAddressSnapshots() {
        givenReadyCartAndCatalog();
        givenApprovedCardPayment();

        checkout.execute(cardCommand());

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        Order order = saved.getValue();
        assertEquals(CUSTOMER_ID, order.customerId());
        assertEquals(PAYMENT_ID, order.paymentId());
        assertEquals(OrderStatus.PENDING, order.status());
        assertEquals("Leche entera", order.items().get(0).productName());
        assertEquals(PRICE, order.items().get(0).unitPrice());
        assertEquals(QUANTITY, order.items().get(0).quantity());
        assertEquals(PRODUCT_ID, order.items().get(0).productId());
        assertEquals("Ada Lovelace", order.shippingAddress().recipientName());
        assertEquals("Calle 1 # 2-3", order.shippingAddress().addressLine());
        assertEquals("Bogotá", order.shippingAddress().city());
        assertEquals("Cundinamarca", order.shippingAddress().department());
        assertEquals("3001234567", order.shippingAddress().phone());
    }

    private void givenReadyCartAndCatalog() {
        lenient().when(shoppingCartPort.getActiveCart(CUSTOMER_ID)).thenReturn(cart());
        lenient().when(customerAddressPort.getAddressForCustomer(CUSTOMER_ID, ADDRESS_ID))
                .thenReturn(Optional.of(address()));
        lenient().when(productCatalogPort.getProduct(PRODUCT_ID)).thenReturn(Optional.of(product(true, true)));
        lenient().when(productCatalogPort.checkAvailability(any())).thenReturn(AvailabilityResult.available());
    }

    private void givenApprovedCardPayment() {
        lenient().when(paymentPort.processPayment(any()))
                .thenReturn(paymentResult(PaymentStatus.APPROVED, "sim-approved"));
        lenient().when(inventoryPort.decreaseStockAtomically(any())).thenReturn(StockDecrementResult.success());
        lenient().when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenPendingCashPayment() {
        lenient().when(paymentPort.processPayment(any()))
                .thenReturn(paymentResult(PaymentStatus.PENDING, "cod-pending"));
        lenient().when(inventoryPort.decreaseStockAtomically(any())).thenReturn(StockDecrementResult.success());
        lenient().when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void verifyNoSideEffects() {
        verify(paymentPort, never()).processPayment(any());
        verify(inventoryPort, never()).decreaseStockAtomically(any());
        verify(orderRepository, never()).save(any());
        verify(shoppingCartPort, never()).clearCart(any());
    }

    private static CheckoutCommand cardCommand() {
        return new CheckoutCommand(
                ADDRESS_ID,
                PaymentMethod.SIMULATED_CARD,
                List.of(new CheckoutItem(PRODUCT_ID, QUANTITY, PRICE)),
                "key-1");
    }

    private static CheckoutCommand codCommand() {
        return new CheckoutCommand(
                ADDRESS_ID,
                PaymentMethod.CASH_ON_DELIVERY,
                List.of(new CheckoutItem(PRODUCT_ID, QUANTITY, PRICE)),
                "key-cod");
    }

    private static CartSnapshot cart() {
        return new CartSnapshot(CART_ID, List.of(new CartItemSnapshot(PRODUCT_ID, QUANTITY, PRICE)));
    }

    private static AddressSnapshot address() {
        return new AddressSnapshot(
                "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567");
    }

    private static ProductCatalogInfo product(boolean active, boolean available) {
        return new ProductCatalogInfo(PRODUCT_ID, "Leche entera", PRICE, available, active);
    }

    private static PaymentResult paymentResult(PaymentStatus status, String providerReference) {
        return new PaymentResult(
                PAYMENT_ID,
                PRICE,
                status == PaymentStatus.PENDING ? PaymentMethod.CASH_ON_DELIVERY : PaymentMethod.SIMULATED_CARD,
                status,
                providerReference,
                null,
                NOW,
                NOW);
    }

    private static final class InMemoryIdempotencyPort implements IdempotencyPort {

        private final Map<String, IdempotencyRecord> records = new HashMap<>();

        @Override
        public Optional<IdempotencyRecord> find(String key, UUID customerId) {
            return Optional.ofNullable(records.get(key + "|" + customerId));
        }

        @Override
        public void save(IdempotencyRecord record) {
            records.put(record.key() + "|" + record.customerId(), record);
        }

        @Override
        public void deleteAllByCustomerId(UUID customerId) {
            records.entrySet().removeIf(entry -> entry.getKey().endsWith("|" + customerId));
        }
    }
}
