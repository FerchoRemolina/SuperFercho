package com.superfercho.orders.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PreviewCustomerExclusionPort;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdvanceOrderLifecycleUseCaseTest {

    private static final Instant CONFIRMED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClockProvider clockProvider;

    @Mock
    private PreviewCustomerExclusionPort previewCustomerExclusionPort;

    private AdvanceOrderLifecycleUseCase advanceLifecycle;

    @BeforeEach
    void setUp() {
        advanceLifecycle =
                new AdvanceOrderLifecycleUseCase(orderRepository, clockProvider, previewCustomerExclusionPort);
        lenient().when(previewCustomerExclusionPort.isPreviewTemporaryCustomer(any())).thenReturn(false);
    }

    @Test
    void shouldLeaveConfirmedThroughSaveIfConfirmed() {
        Order confirmed = confirmedOrder("ORD-1");
        when(clockProvider.currentTime()).thenReturn(CONFIRMED_AT.plusSeconds(120));
        when(orderRepository.findInProgressForLifecycle()).thenReturn(List.of(confirmed));
        when(orderRepository.saveIfConfirmed(any())).thenAnswer(call -> Optional.of(call.getArgument(0)));

        List<OrderResult> advanced = advanceLifecycle.execute();

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveIfConfirmed(saved.capture());
        verify(orderRepository, never()).save(any());
        assertEquals(OrderStatus.PREPARING, saved.getValue().status());
        assertEquals(1, advanced.size());
        assertEquals(confirmed.id(), advanced.get(0).id());
        assertEquals(OrderStatus.PREPARING, advanced.get(0).status());
    }

    @Test
    void shouldSkipConfirmedOrderWhenTransitionIsLost() {
        when(clockProvider.currentTime()).thenReturn(CONFIRMED_AT.plusSeconds(120));
        when(orderRepository.findInProgressForLifecycle()).thenReturn(List.of(confirmedOrder("ORD-1")));
        when(orderRepository.saveIfConfirmed(any())).thenReturn(Optional.empty());

        List<OrderResult> advanced = advanceLifecycle.execute();

        assertEquals(List.of(), advanced);
        verify(orderRepository).saveIfConfirmed(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldAdvanceAlreadyStartedOrdersThroughPlainSave() {
        Order preparing = confirmedOrder("ORD-1").startPreparation(CONFIRMED_AT.plusSeconds(120));
        when(clockProvider.currentTime()).thenReturn(CONFIRMED_AT.plusSeconds(240));
        when(orderRepository.findInProgressForLifecycle()).thenReturn(List.of(preparing));
        when(orderRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        List<OrderResult> advanced = advanceLifecycle.execute();

        verify(orderRepository, never()).saveIfConfirmed(any());
        assertEquals(1, advanced.size());
        assertEquals(OrderStatus.DELIVERY, advanced.get(0).status());
    }

    @Test
    void shouldCatchUpToDeliveredWhenJobIsLate() {
        Order preparing = confirmedOrder("ORD-1").startPreparation(CONFIRMED_AT.plusSeconds(120));
        when(clockProvider.currentTime()).thenReturn(CONFIRMED_AT.plusSeconds(3600));
        when(orderRepository.findInProgressForLifecycle()).thenReturn(List.of(preparing));
        when(orderRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        List<OrderResult> advanced = advanceLifecycle.execute();

        assertEquals(1, advanced.size());
        assertEquals(OrderStatus.DELIVERED, advanced.get(0).status());
    }

    @Test
    void shouldSkipOrdersAlreadyAtTargetStatus() {
        Order confirmed = confirmedOrder("ORD-1");
        when(clockProvider.currentTime()).thenReturn(CONFIRMED_AT.plusSeconds(60));
        when(orderRepository.findInProgressForLifecycle()).thenReturn(List.of(confirmed));

        List<OrderResult> advanced = advanceLifecycle.execute();

        assertEquals(List.of(), advanced);
        verify(orderRepository, never()).saveIfConfirmed(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldSkipPreviewTemporaryCustomers() {
        when(clockProvider.currentTime()).thenReturn(CONFIRMED_AT.plusSeconds(120));
        when(orderRepository.findInProgressForLifecycle()).thenReturn(List.of(confirmedOrder("ORD-1")));
        when(previewCustomerExclusionPort.isPreviewTemporaryCustomer(CUSTOMER_ID)).thenReturn(true);

        List<OrderResult> advanced = advanceLifecycle.execute();

        assertEquals(List.of(), advanced);
        verify(orderRepository, never()).saveIfConfirmed(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldAdvanceOnlyTheOrdersThatReachedTheirNextStep() {
        Order due = confirmedOrder("ORD-DUE");
        Order tooRecent = order("ORD-RECENT", CONFIRMED_AT.plusSeconds(90));
        when(clockProvider.currentTime()).thenReturn(CONFIRMED_AT.plusSeconds(120));
        when(orderRepository.findInProgressForLifecycle()).thenReturn(List.of(due, tooRecent));
        when(orderRepository.saveIfConfirmed(any())).thenAnswer(call -> Optional.of(call.getArgument(0)));

        List<OrderResult> advanced = advanceLifecycle.execute();

        assertEquals(1, advanced.size());
        assertEquals(due.id(), advanced.get(0).id());
        verify(orderRepository, times(1)).saveIfConfirmed(any());
    }

    @Test
    void shouldDoNothingWhenNoInProgressOrdersExist() {
        when(clockProvider.currentTime()).thenReturn(CONFIRMED_AT);
        when(orderRepository.findInProgressForLifecycle()).thenReturn(List.of());

        assertEquals(List.of(), advanceLifecycle.execute());
        verify(orderRepository, never()).saveIfConfirmed(any());
        verify(orderRepository, never()).save(any());
    }

    private static Order confirmedOrder(String orderNumber) {
        return order(orderNumber, CONFIRMED_AT);
    }

    private static Order order(String orderNumber, Instant createdAt) {
        return Order.create(
                UUID.randomUUID(),
                new OrderNumber(orderNumber),
                CUSTOMER_ID,
                List.of(OrderItem.create(
                        UUID.randomUUID(), PRODUCT_ID, "Leche entera", Money.cop(new BigDecimal("10.50")), 2)),
                new ShippingAddressSnapshot(
                        "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567"),
                null,
                createdAt,
                createdAt);
    }
}
