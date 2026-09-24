package com.superfercho.catalog.application.exception;

import java.util.UUID;

public class InvalidProductVariantReferenceException extends RuntimeException {

    public InvalidProductVariantReferenceException() {
        super("Invalid product variant reference");
    }

    public InvalidProductVariantReferenceException(UUID productVariantId) {
        super("Invalid product variant reference: " + productVariantId);
    }

    public InvalidProductVariantReferenceException(String message) {
        super(message);
    }
}
