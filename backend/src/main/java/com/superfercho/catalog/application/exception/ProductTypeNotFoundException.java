package com.superfercho.catalog.application.exception;

import java.util.UUID;

public class ProductTypeNotFoundException extends RuntimeException {

    public ProductTypeNotFoundException(UUID productTypeId) {
        super("Product type not found: " + productTypeId);
    }
}
