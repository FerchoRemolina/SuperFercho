package com.superfercho.orders.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.UpdateOrderStatusCommand;
import com.superfercho.orders.application.exception.InvalidOrderStatusUpdateException;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PreviewCustomerExclusionPort;
import com.superfercho.orders.domain.exception.InvalidOrderStateTransitionException;
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
class UpdateOrderStatusUseCaseTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-03-01T10:20:00Z");
    private static final UUID ORDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClockProvider clockProvider;

    @Mock
    private PreviewCustomerExclusionPort previewCustomerExclusionPort;

    private UpdateOrderStatusUseCase updateOrderStatus;

    @BeforeEach
    void setUp() {
        updateOrderStatus =
                new UpdateOrderStatusUseCase(orderRepository, clockProvider, previewCustomerExclusionPort);
        when(clockProvider.currentTime()).thenReturn(NOW);
        lenient().when(previewCustomerExclusionPort.isPreviewTemporaryCustomer(any())).thenReturn(false);
        lenient().when(orderRepository.saveIfPending(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
        lenient().when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldConfirmPendingOrder() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        OrderResult result = updateOrderStatus.execute(new UpdateOrderStatusCommand(ORDER_ID, OrderStatus.CONFIRMED));

        assertEquals(OrderStatus.CONFIRMED, result.status());
        assertEquals(NOW, result.confirmedAt());
    }

    @Test
    void shouldStartPreparationFromConfirmed() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder().confirm(NOW)));

        OrderResult result = updateOrderStatus.execute(new UpdateOrderStatusCommand(ORDER_ID, OrderStatus.PREPARING));

        assertEquals(OrderStatus.PREPARING, result.status());
    }

    @Test
    void shouldMarkReadyFromPreparing() {
        when(orderRepository.findById(ORDER_ID))
                .thenReturn(Optional.of(pendingOrder().confirm(NOW).startPreparation(NOW)));

        OrderResult result = updateOrderStatus.execute(new UpdateOrderStatusCommand(ORDER_ID, OrderStatus.READY));

        assertEquals(OrderStatus.READY, result.status());
    }

    @Test
    void shouldMarkDeliveredFromReady() {
        when(orderRepository.findById(ORDER_ID))
                .thenReturn(Optional.of(pendingOrder().confirm(NOW).startPreparation(NOW).markReady(NOW)));

        OrderResult result = updateOrderStatus.execute(new UpdateOrderStatusCommand(ORDER_ID, OrderStatus.DELIVERED));

        assertEquals(OrderStatus.DELIVERED, result.status());
    }

    @Test
    void shouldDelegateInvalidAggregateTransition() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> updateOrderStatus.execute(new UpdateOrderStatusCommand(ORDER_ID, OrderStatus.PREPARING)));
        verify(orderRepository, never()).saveIfPending(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldRejectCancelledAsAdministrativeTarget() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        assertThrows(
                InvalidOrderStatusUpdateException.class,
                () -> updateOrderStatus.execute(new UpdateOrderStatusCommand(ORDER_ID, OrderStatus.CANCELLED)));
        verify(orderRepository, never()).saveIfPending(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldRejectConfirmWhenPendingTransitionIsLost() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));
        doReturn(Optional.empty()).when(orderRepository).saveIfPending(any());

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> updateOrderStatus.execute(new UpdateOrderStatusCommand(ORDER_ID, OrderStatus.CONFIRMED)));
        verify(orderRepository, never()).save(any());
    }

    private static Order pendingOrder() {
        return Order.create(
                ORDER_ID,
                new OrderNumber("ORD-1001"),
                CUSTOMER_ID,
                List.of(OrderItem.create(
                        UUID.fromString("99999999-9999-9999-9999-999999999999"),
                        PRODUCT_ID,
                        "Leche entera",
                        Money.cop(new BigDecimal("10.50")),
                        2)),
                new ShippingAddressSnapshot(
                        "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567"),
                null,
                CREATED_AT,
                CREATED_AT);
    }
}
