package com.superfercho.orders.application.usecase;

import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cancels pending preview orders (restoring stock and refunding approved payments), then deletes
 * all orders, payments, and checkout idempotency rows for the temporary customer.
 */
public final class CancelAndDeletePreviewOrdersUseCase {

    private static final int PAGE_SIZE = PageRequest.MAX_SIZE;

    private final OrderRepository orderRepository;
    private final InventoryPort inventoryPort;
    private final PaymentPort paymentPort;
    private final IdempotencyPort idempotencyPort;
    private final ClockProvider clockProvider;

    public CancelAndDeletePreviewOrdersUseCase(
            OrderRepository orderRepository,
            InventoryPort inventoryPort,
            PaymentPort paymentPort,
            IdempotencyPort idempotencyPort,
            ClockProvider clockProvider) {
        this.orderRepository = orderRepository;
        this.inventoryPort = inventoryPort;
        this.paymentPort = paymentPort;
        this.idempotencyPort = idempotencyPort;
        this.clockProvider = clockProvider;
    }

    public void execute(UUID customerId) {
        List<Order> orders = loadAllOrders(customerId);
        Set<UUID> paymentIds = new HashSet<>();
        for (Order order : orders) {
            if (order.paymentId() != null) {
                paymentIds.add(order.paymentId());
            }
            if (order.status() == OrderStatus.PENDING) {
                cancelPendingForCleanup(order);
            }
        }
        for (UUID paymentId : paymentIds) {
            paymentPort.deletePayment(paymentId);
        }
        orderRepository.deleteAllByCustomerId(customerId);
        idempotencyPort.deleteAllByCustomerId(customerId);
    }

    private void cancelPendingForCleanup(Order order) {
        Order cancelled = order.cancelForCleanup(clockProvider.currentTime());
        orderRepository.saveIfPending(cancelled).ifPresent(saved -> {
            inventoryPort.restoreStock(order.items().stream()
                    .map(item -> new StockQuantity(item.productId(), item.quantity()))
                    .toList());
            if (order.paymentId() != null
                    && paymentPort.getPayment(order.paymentId()).status() == PaymentStatus.APPROVED) {
                paymentPort.refundPayment(order.paymentId());
            }
        });
    }

    private List<Order> loadAllOrders(UUID customerId) {
        List<Order> orders = new ArrayList<>();
        int page = 0;
        while (true) {
            PagedResult<Order> result =
                    orderRepository.findByCustomerId(customerId, new PageRequest(page, PAGE_SIZE));
            orders.addAll(result.items());
            long loaded = (long) page * PAGE_SIZE + result.items().size();
            if (result.items().isEmpty() || loaded >= result.totalElements()) {
                break;
            }
            page++;
        }
        return orders;
    }
}
