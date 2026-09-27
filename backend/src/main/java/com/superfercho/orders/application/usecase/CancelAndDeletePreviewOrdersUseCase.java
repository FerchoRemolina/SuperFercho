package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.model.Order;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Preview cleanup: deletes temporary-customer orders, payments, and idempotency rows.
 * Preview never consumes real inventory, so cleanup must not restore stock.
 */
public final class CancelAndDeletePreviewOrdersUseCase {

    private static final int PAGE_SIZE = PageRequest.MAX_SIZE;

    private final OrderRepository orderRepository;
    private final PaymentPort paymentPort;
    private final IdempotencyPort idempotencyPort;

    public CancelAndDeletePreviewOrdersUseCase(
            OrderRepository orderRepository, PaymentPort paymentPort, IdempotencyPort idempotencyPort) {
        this.orderRepository = orderRepository;
        this.paymentPort = paymentPort;
        this.idempotencyPort = idempotencyPort;
    }

    public void execute(UUID customerId) {
        List<Order> orders = loadAllOrders(customerId);
        Set<UUID> paymentIds = new HashSet<>();
        for (Order order : orders) {
            if (order.paymentId() != null) {
                paymentIds.add(order.paymentId());
            }
        }
        for (UUID paymentId : paymentIds) {
            paymentPort.deletePayment(paymentId);
        }
        orderRepository.deleteAllByCustomerId(customerId);
        idempotencyPort.deleteAllByCustomerId(customerId);
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
