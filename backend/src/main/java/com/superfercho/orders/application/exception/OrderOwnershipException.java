package com.superfercho.orders.application.exception;

import java.util.UUID;

public class OrderOwnershipException extends RuntimeException {

    private final UUID customerId;
    private final UUID orderId;

    public OrderOwnershipException(UUID customerId, UUID orderId) {
        super("Order does not belong to the current customer");
        this.customerId = customerId;
        this.orderId = orderId;
    }

    public UUID customerId() {
        return customerId;
    }

    public UUID orderId() {
        return orderId;
    }
}
