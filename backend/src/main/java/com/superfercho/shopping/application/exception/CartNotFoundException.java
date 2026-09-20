package com.superfercho.shopping.application.exception;

import java.util.UUID;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(UUID customerId) {
        super("Cart not found for customer: " + customerId);
    }
}
