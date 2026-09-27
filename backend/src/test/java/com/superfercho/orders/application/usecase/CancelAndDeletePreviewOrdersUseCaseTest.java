package com.superfercho.orders.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelAndDeletePreviewOrdersUseCaseTest {

    private static final Instant CONFIRMED_AT = Instant.parse("2026-06-01T11:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentPort paymentPort;

    @Mock
    private IdempotencyPort idempotencyPort;

    @Test
    void deletesInProgressPreviewOrdersWithoutRestoringStockOrCancelling() {
        UUID customerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Order confirmed = confirmedOrder(customerId, paymentId);
        when(orderRepository.findByCustomerId(eq(customerId), any(PageRequest.class)))
                .thenReturn(new PagedResult<>(List.of(confirmed), 0, 50, 1));

        useCase().execute(customerId);

        verify(orderRepository, never()).save(any());
        verify(paymentPort, never()).refundPayment(any());
        verify(paymentPort).deletePayment(paymentId);
        verify(orderRepository).deleteAllByCustomerId(customerId);
        verify(idempotencyPort).deleteAllByCustomerId(customerId);
    }

    @Test
    void deletesPreviewOrdersInAnyLifecycleState() {
        UUID customerId = UUID.randomUUID();
        Order preparing = confirmedOrder(customerId, null).startPreparation(CONFIRMED_AT.plusSeconds(120));
        Order delivery = preparing.startDelivery(CONFIRMED_AT.plusSeconds(240));
        Order delivered = delivery.markDelivered(CONFIRMED_AT.plusSeconds(360));
        Order cancelled = confirmedOrder(customerId, null).cancel(CONFIRMED_AT);
        when(orderRepository.findByCustomerId(eq(customerId), any(PageRequest.class)))
                .thenReturn(new PagedResult<>(List.of(preparing, delivery, delivered, cancelled), 0, 50, 4));

        useCase().execute(customerId);

        verify(orderRepository, never()).save(any());
        verify(orderRepository).deleteAllByCustomerId(customerId);
        verify(idempotencyPort).deleteAllByCustomerId(customerId);
    }

    @Test
    void isIdempotentWhenCustomerAlreadyHasNoOrders() {
        UUID customerId = UUID.randomUUID();
        when(orderRepository.findByCustomerId(eq(customerId), any(PageRequest.class)))
                .thenReturn(new PagedResult<>(List.of(), 0, 50, 0));

        useCase().execute(customerId);

        verify(paymentPort, never()).deletePayment(any());
        verify(orderRepository).deleteAllByCustomerId(customerId);
        verify(idempotencyPort).deleteAllByCustomerId(customerId);
    }

    private CancelAndDeletePreviewOrdersUseCase useCase() {
        return new CancelAndDeletePreviewOrdersUseCase(orderRepository, paymentPort, idempotencyPort);
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
