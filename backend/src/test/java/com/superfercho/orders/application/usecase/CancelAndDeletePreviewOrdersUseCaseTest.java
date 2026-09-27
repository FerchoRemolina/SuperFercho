package com.superfercho.orders.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelAndDeletePreviewOrdersUseCaseTest {

    private static final Instant CONFIRMED_AT = Instant.parse("2026-06-01T11:00:00Z");
    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryPort inventoryPort;

    @Mock
    private PaymentPort paymentPort;

    @Mock
    private IdempotencyPort idempotencyPort;

    @Mock
    private ClockProvider clockProvider;

    @Test
    void cancelsConfirmedOrderForCleanupRestoresStockRefundsAndDeletes() {
        UUID customerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Order confirmed = confirmedOrder(customerId, paymentId);
        when(clockProvider.currentTime()).thenReturn(NOW);
        when(orderRepository.findByCustomerId(eq(customerId), any(PageRequest.class)))
                .thenReturn(new PagedResult<>(List.of(confirmed), 0, 50, 1));
        when(paymentPort.getPayment(paymentId)).thenReturn(approvedPayment(paymentId));

        useCase().execute(customerId);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(saved.getValue().cancelledAt()).isEqualTo(NOW);
        verify(inventoryPort).restoreStock(List.of(new StockQuantity(PRODUCT_ID, 2)));
        verify(paymentPort).refundPayment(paymentId);
        verify(paymentPort).deletePayment(paymentId);
        verify(orderRepository).deleteAllByCustomerId(customerId);
        verify(idempotencyPort).deleteAllByCustomerId(customerId);
    }

    @Test
    void cancelsInProgressOrdersIgnoringTheCustomerCancellationWindow() {
        UUID customerId = UUID.randomUUID();
        Order preparing = confirmedOrder(customerId, null).startPreparation(CONFIRMED_AT.plusSeconds(120));
        Order delivery = preparing.startDelivery(CONFIRMED_AT.plusSeconds(240));
        when(clockProvider.currentTime()).thenReturn(NOW);
        when(orderRepository.findByCustomerId(eq(customerId), any(PageRequest.class)))
                .thenReturn(new PagedResult<>(List.of(preparing, delivery), 0, 50, 2));

        useCase().execute(customerId);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).allMatch(order -> order.status() == OrderStatus.CANCELLED);
        verify(orderRepository).deleteAllByCustomerId(customerId);
    }

    @Test
    void deletesTerminalOrdersWithoutCancellingThem() {
        UUID customerId = UUID.randomUUID();
        Order delivered = confirmedOrder(customerId, null)
                .startPreparation(CONFIRMED_AT.plusSeconds(120))
                .startDelivery(CONFIRMED_AT.plusSeconds(240))
                .markDelivered(CONFIRMED_AT.plusSeconds(360));
        Order cancelled = confirmedOrder(customerId, null).cancel(CONFIRMED_AT);
        when(orderRepository.findByCustomerId(eq(customerId), any(PageRequest.class)))
                .thenReturn(new PagedResult<>(List.of(delivered, cancelled), 0, 50, 2));

        useCase().execute(customerId);

        verify(orderRepository, never()).save(any());
        verify(inventoryPort, never()).restoreStock(any());
        verify(paymentPort, never()).refundPayment(any());
        verify(orderRepository).deleteAllByCustomerId(customerId);
        verify(idempotencyPort).deleteAllByCustomerId(customerId);
    }

    private CancelAndDeletePreviewOrdersUseCase useCase() {
        return new CancelAndDeletePreviewOrdersUseCase(
                orderRepository, inventoryPort, paymentPort, idempotencyPort, clockProvider);
    }

    private static PaymentResult approvedPayment(UUID paymentId) {
        return new PaymentResult(
                paymentId,
                Money.cop(new BigDecimal("21.00")),
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.APPROVED,
                "sim-1",
                null,
                CONFIRMED_AT,
                CONFIRMED_AT);
    }

    private static Order confirmedOrder(UUID customerId, UUID paymentId) {
        return Order.create(
                UUID.randomUUID(),
                new OrderNumber("ORD-PREVIEW-1"),
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
                CONFIRMED_AT,
                CONFIRMED_AT);
    }
}
