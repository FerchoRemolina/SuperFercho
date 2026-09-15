package com.superfercho.orders.application.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String key) {
        super("Idempotency key already used with a different checkout request: " + key);
    }
}
