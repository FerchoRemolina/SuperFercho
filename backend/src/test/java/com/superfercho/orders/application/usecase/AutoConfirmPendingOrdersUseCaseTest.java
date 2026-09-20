package com.superfercho.orders.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
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
class AutoConfirmPendingOrdersUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:20:00Z");
    private static final Instant ELIGIBLE_CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant TOO_RECENT_CREATED_AT = Instant.parse("2026-03-01T10:10:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClockProvider clockProvider;

    private AutoConfirmPendingOrdersUseCase autoConfirm;

    @BeforeEach
    void setUp() {
        autoConfirm = new AutoConfirmPendingOrdersUseCase(orderRepository, clockProvider);
        when(clockProvider.currentTime()).thenReturn(NOW);
    }

    @Test
    void shouldConfirmOnlyEligiblePendingOrders() {
        Order eligible = pendingOrder(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "ORD-ELIGIBLE", ELIGIBLE_CREATED_AT);
        Order tooRecent = pendingOrder(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "ORD-RECENT", TOO_RECENT_CREATED_AT);
        when(orderRepository.findPendingOrdersEligibleForAutomaticConfirmation(NOW))
                .thenReturn(List.of(eligible, tooRecent));
        when(orderRepository.saveIfPending(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

        List<OrderResult> confirmed = autoConfirm.execute();

        assertEquals(1, confirmed.size());
        assertEquals(eligible.id(), confirmed.get(0).id());
        assertEquals(OrderStatus.CONFIRMED, confirmed.get(0).status());
        verify(orderRepository, times(1)).saveIfPending(any());
    }

    @Test
    void shouldNotSaveWhenRepositoryReturnsNoEligibleOrders() {
        when(orderRepository.findPendingOrdersEligibleForAutomaticConfirmation(NOW)).thenReturn(List.of());

        List<OrderResult> confirmed = autoConfirm.execute();

        assertEquals(List.of(), confirmed);
        verify(orderRepository, never()).saveIfPending(any());
    }

    @Test
    void shouldSkipOrderWhenPendingTransitionIsLost() {
        Order eligible = pendingOrder(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "ORD-ELIGIBLE", ELIGIBLE_CREATED_AT);
        when(orderRepository.findPendingOrdersEligibleForAutomaticConfirmation(NOW)).thenReturn(List.of(eligible));
        when(orderRepository.saveIfPending(any())).thenReturn(Optional.empty());

        List<OrderResult> confirmed = autoConfirm.execute();

        assertEquals(List.of(), confirmed);
        verify(orderRepository).saveIfPending(any());
    }

    private static Order pendingOrder(UUID orderId, String orderNumber, Instant createdAt) {
        return Order.create(
                orderId,
                new OrderNumber(orderNumber),
                CUSTOMER_ID,
                List.of(OrderItem.create(
                        UUID.randomUUID(),
                        PRODUCT_ID,
                        "Leche entera",
                        Money.cop(new BigDecimal("10.50")),
                        2)),
                new ShippingAddressSnapshot(
                        "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567"),
                null,
                createdAt,
                createdAt);
    }
}
