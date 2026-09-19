package com.superfercho.orders.application.usecase;

import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.CurrentUserProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.exception.InvalidOrderStateTransitionException;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;

public final class CancelOrderUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final ClockProvider clockProvider;
    private final OrderRepository orderRepository;
    private final InventoryPort inventoryPort;
    private final PaymentPort paymentPort;

    public CancelOrderUseCase(
            CurrentUserProvider currentUserProvider,
            ClockProvider clockProvider,
            OrderRepository orderRepository,
            InventoryPort inventoryPort,
            PaymentPort paymentPort) {
        this.currentUserProvider = currentUserProvider;
        this.clockProvider = clockProvider;
        this.orderRepository = orderRepository;
        this.inventoryPort = inventoryPort;
        this.paymentPort = paymentPort;
    }

    public OrderResult execute(CancelOrderCommand command) {
        Order order = OwnedOrderAccess.requireOwnedOrder(
                orderRepository, currentUserProvider.getCurrentUserId(), command.orderId());
        Order cancelled = order.cancel(clockProvider.currentTime());
        Order saved = orderRepository
                .saveIfPending(cancelled)
                .orElseThrow(() -> new InvalidOrderStateTransitionException(OrderStatus.PENDING, OrderStatus.CANCELLED));
        inventoryPort.restoreStock(order.items().stream()
                .map(item -> new StockQuantity(item.productId(), item.quantity()))
                .toList());
        if (order.paymentId() != null
                && paymentPort.getPayment(order.paymentId()).status() == PaymentStatus.APPROVED) {
            paymentPort.refundPayment(order.paymentId());
        }
        return OrderResult.from(saved);
    }
}
