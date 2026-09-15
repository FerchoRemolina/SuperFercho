package com.superfercho.orders.domain.exception;

public class OrderCancellationNotAllowedException extends RuntimeException {

    public OrderCancellationNotAllowedException(String message) {
        super(message);
    }
}
