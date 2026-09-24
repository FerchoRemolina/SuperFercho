package com.superfercho.catalog.application.exception;

public class DuplicateProductVariantException extends RuntimeException {

    public DuplicateProductVariantException() {
        super("A product variant with this name already exists for the product type");
    }
}
