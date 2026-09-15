package com.superfercho.orders.application.exception;

import java.util.UUID;

public class ProductNotAvailableException extends RuntimeException {

    public ProductNotAvailableException(UUID productId) {
        super("Product is not available: " + productId);
    }
}
