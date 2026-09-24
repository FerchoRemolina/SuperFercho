package com.superfercho.catalog.application.exception;

import java.util.UUID;

public class InvalidProductTypeReferenceException extends RuntimeException {

    public InvalidProductTypeReferenceException() {
        super("Invalid product type reference");
    }

    public InvalidProductTypeReferenceException(UUID productTypeId) {
        super("Invalid product type reference: " + productTypeId);
    }
}
