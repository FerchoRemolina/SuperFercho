package com.superfercho.orders.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.dto.UpdateOrderStatusCommand;
import com.superfercho.orders.application.exception.PreviewCustomerOrderUpdateNotAllowedException;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelAndDeletePreviewOrdersUseCaseTest {

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

    @Mock
    private PreviewCustomerExclusionPort previewCustomerExclusionPort;

    @Test
    void cancelsPendingRestoresStockRefundsAndDeletes() {
        UUID customerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Order pending = pendingOrder(customerId, paymentId);
        when(clockProvider.currentTime()).thenReturn(NOW);
        when(orderRepository.findByCustomerId(eq(customerId), any(PageRequest.class)))
                .thenReturn(new PagedResult<>(List.of(pending), 0, 50, 1));
        when(orderRepository.saveIfPending(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
        when(paymentPort.getPayment(paymentId))
                .thenReturn(new PaymentResult(
                        paymentId,
                        Money.cop(new BigDecimal("21.00")),
                        PaymentMethod.SIMULATED_CARD,
                        PaymentStatus.APPROVED,
                        "sim-1",
                        null,
                        NOW,
                        NOW));

        new CancelAndDeletePreviewOrdersUseCase(
                        orderRepository, inventoryPort, paymentPort, idempotencyPort, clockProvider)
                .execute(customerId);

        verify(inventoryPort).restoreStock(any());
        verify(paymentPort).refundPayment(paymentId);
        verify(paymentPort).deletePayment(paymentId);
        verify(orderRepository).deleteAllByCustomerId(customerId);
        verify(idempotencyPort).deleteAllByCustomerId(customerId);
    }

    @Test
    void updateOrderStatusRejectsPreviewTemporaryCustomer() {
        UUID customerId = UUID.randomUUID();
        Order pending = pendingOrder(customerId, UUID.randomUUID());
        when(orderRepository.findById(pending.id())).thenReturn(Optional.of(pending));
        when(previewCustomerExclusionPort.isPreviewTemporaryCustomer(customerId)).thenReturn(true);

        UpdateOrderStatusUseCase useCase =
                new UpdateOrderStatusUseCase(orderRepository, clockProvider, previewCustomerExclusionPort);

        assertThatThrownBy(
                        () -> useCase.execute(new UpdateOrderStatusCommand(pending.id(), OrderStatus.CONFIRMED)))
                .isInstanceOf(PreviewCustomerOrderUpdateNotAllowedException.class);
        verify(orderRepository, never()).save(any());
        verify(orderRepository, never()).saveIfPending(any());
    }

    @Test
    void autoConfirmSkipsPreviewTemporaryCustomer() {
        UUID customerId = UUID.randomUUID();
        Order pending = pendingOrder(customerId, UUID.randomUUID());
        Instant eligibleAt = pending.createdAt().plus(Order.CUSTOMER_CANCELLATION_WINDOW).plusSeconds(1);
        when(clockProvider.currentTime()).thenReturn(eligibleAt);
        when(orderRepository.findPendingOrdersEligibleForAutomaticConfirmation(eligibleAt))
                .thenReturn(List.of(pending));
        when(previewCustomerExclusionPort.isPreviewTemporaryCustomer(customerId)).thenReturn(true);

        List<?> confirmed = new AutoConfirmPendingOrdersUseCase(
                        orderRepository, clockProvider, previewCustomerExclusionPort)
                .execute();

        assertThat(confirmed).isEmpty();
        verify(orderRepository, never()).saveIfPending(any());
    }

    private static Order pendingOrder(UUID customerId, UUID paymentId) {
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
                NOW.minusSeconds(60),
                NOW.minusSeconds(60));
    }
}
