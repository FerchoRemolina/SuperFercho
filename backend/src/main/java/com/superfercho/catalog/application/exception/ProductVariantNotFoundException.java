package com.superfercho.catalog.application.exception;

import java.util.UUID;

public class ProductVariantNotFoundException extends RuntimeException {

    public ProductVariantNotFoundException(UUID productVariantId) {
        super("Product variant not found: " + productVariantId);
    }
}
