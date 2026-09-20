package com.superfercho.orders.application.exception;

import com.superfercho.orders.domain.model.OrderStatus;

public class InvalidOrderStatusUpdateException extends RuntimeException {

    public InvalidOrderStatusUpdateException(OrderStatus status) {
        super("Order status cannot be updated to: " + status);
    }
}
