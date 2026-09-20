package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.exception.OrderOwnershipException;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import java.util.UUID;

final class OwnedOrderAccess {

    private OwnedOrderAccess() {
    }

    static Order requireOwnedOrder(OrderRepository orders, UUID customerId, UUID orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.customerId().equals(customerId)) {
            throw new OrderOwnershipException(customerId, orderId);
        }
        return order;
    }
}
