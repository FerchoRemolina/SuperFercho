package com.superfercho.catalog.application.exception;

import java.util.UUID;

public class ProductStockConflictException extends RuntimeException {

    public ProductStockConflictException(UUID productId) {
        super("Product stock changed concurrently: " + productId);
    }
}
