package com.superfercho.orders.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.exception.OrderOwnershipException;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.CurrentUserProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.exception.InvalidOrderStateTransitionException;
import com.superfercho.orders.domain.exception.OrderCancellationNotAllowedException;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelOrderUseCaseTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant WITHIN_WINDOW = Instant.parse("2026-03-01T10:10:00Z");
    private static final Instant AFTER_WINDOW = Instant.parse("2026-03-01T10:15:00.001Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ORDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PAYMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ClockProvider clockProvider;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryPort inventoryPort;

    @Mock
    private PaymentPort paymentPort;

    private CancelOrderUseCase cancelOrder;

    @BeforeEach
    void setUp() {
        cancelOrder = new CancelOrderUseCase(
                currentUserProvider, clockProvider, orderRepository, inventoryPort, paymentPort);
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
    }

    @Test
    void shouldCancelOwnedOrderWithinWindow() {
        Order order = pendingOrder(CUSTOMER_ID, PAYMENT_ID);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(clockProvider.currentTime()).thenReturn(WITHIN_WINDOW);
        when(orderRepository.saveIfPending(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
        when(paymentPort.getPayment(PAYMENT_ID))
                .thenReturn(paymentResult(PaymentStatus.APPROVED, "sim-1"));

        OrderResult result = cancelOrder.execute(new CancelOrderCommand(ORDER_ID));

        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals(WITHIN_WINDOW, result.cancelledAt());
        verify(inventoryPort).restoreStock(List.of(new StockQuantity(PRODUCT_ID, 2)));
        verify(paymentPort).refundPayment(PAYMENT_ID);
    }

    @Test
    void shouldRejectCancellationOfAnotherCustomersOrder() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder(OTHER_CUSTOMER_ID, PAYMENT_ID)));

        assertThrows(
                OrderOwnershipException.class, () -> cancelOrder.execute(new CancelOrderCommand(ORDER_ID)));
        verify(inventoryPort, never()).restoreStock(any());
        verify(paymentPort, never()).refundPayment(any());
    }

    @Test
    void shouldRejectCancellationOutsideWindow() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder(CUSTOMER_ID, PAYMENT_ID)));
        when(clockProvider.currentTime()).thenReturn(AFTER_WINDOW);

        assertThrows(
                OrderCancellationNotAllowedException.class,
                () -> cancelOrder.execute(new CancelOrderCommand(ORDER_ID)));
        verify(orderRepository, never()).saveIfPending(any());
        verify(inventoryPort, never()).restoreStock(any());
        verify(paymentPort, never()).refundPayment(any());
    }

    @Test
    void shouldRestoreStockWhenCancellationSucceeds() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder(CUSTOMER_ID, PAYMENT_ID)));
        when(clockProvider.currentTime()).thenReturn(WITHIN_WINDOW);
        when(orderRepository.saveIfPending(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
        when(paymentPort.getPayment(PAYMENT_ID))
                .thenReturn(paymentResult(PaymentStatus.APPROVED, "sim-1"));

        cancelOrder.execute(new CancelOrderCommand(ORDER_ID));

        verify(inventoryPort).restoreStock(List.of(new StockQuantity(PRODUCT_ID, 2)));
    }

    @Test
    void shouldRefundApprovedSimulatedCardPayment() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder(CUSTOMER_ID, PAYMENT_ID)));
        when(clockProvider.currentTime()).thenReturn(WITHIN_WINDOW);
        when(orderRepository.saveIfPending(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
        when(paymentPort.getPayment(PAYMENT_ID))
                .thenReturn(paymentResult(PaymentStatus.APPROVED, "sim-1"));

        cancelOrder.execute(new CancelOrderCommand(ORDER_ID));

        verify(paymentPort).refundPayment(PAYMENT_ID);
    }

    @Test
    void shouldNotRefundCashOnDelivery() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder(CUSTOMER_ID, PAYMENT_ID)));
        when(clockProvider.currentTime()).thenReturn(WITHIN_WINDOW);
        when(orderRepository.saveIfPending(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
        when(paymentPort.getPayment(PAYMENT_ID))
                .thenReturn(paymentResult(PaymentStatus.PENDING, "cod-1"));

        cancelOrder.execute(new CancelOrderCommand(ORDER_ID));

        verify(paymentPort, never()).refundPayment(any());
        verify(inventoryPort).restoreStock(List.of(new StockQuantity(PRODUCT_ID, 2)));
    }

    @Test
    void shouldRejectCancellationWhenPendingTransitionIsLost() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder(CUSTOMER_ID, PAYMENT_ID)));
        when(clockProvider.currentTime()).thenReturn(WITHIN_WINDOW);
        when(orderRepository.saveIfPending(any())).thenReturn(Optional.empty());

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> cancelOrder.execute(new CancelOrderCommand(ORDER_ID)));
        verify(inventoryPort, never()).restoreStock(any());
        verify(paymentPort, never()).refundPayment(any());
        verify(paymentPort, never()).getPayment(any());
    }

    private static Order pendingOrder(UUID customerId, UUID paymentId) {
        return Order.create(
                ORDER_ID,
                new OrderNumber("ORD-1001"),
                customerId,
                List.of(OrderItem.create(
                        UUID.fromString("99999999-9999-9999-9999-999999999999"),
                        PRODUCT_ID,
                        "Leche entera",
                        Money.cop(new BigDecimal("10.50")),
                        2)),
                new ShippingAddressSnapshot(
                        "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567"),
                paymentId,
                CREATED_AT,
                CREATED_AT);
    }

    private static PaymentResult paymentResult(PaymentStatus status, String providerReference) {
        return new PaymentResult(
                PAYMENT_ID,
                Money.cop(new BigDecimal("21.00")),
                status == PaymentStatus.PENDING ? PaymentMethod.CASH_ON_DELIVERY : PaymentMethod.SIMULATED_CARD,
                status,
                providerReference,
                null,
                CREATED_AT,
                CREATED_AT);
    }
}
