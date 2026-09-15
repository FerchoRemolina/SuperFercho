package com.superfercho.orders.domain.exception;

import com.superfercho.orders.domain.model.OrderStatus;

public class InvalidOrderStateTransitionException extends RuntimeException {

    public InvalidOrderStateTransitionException(OrderStatus from, OrderStatus to) {
        super("Invalid order state transition: " + from + " -> " + to);
    }
}
