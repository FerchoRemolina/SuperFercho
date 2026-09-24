package com.superfercho.catalog.domain.exception;

public class InvalidProductVariantException extends RuntimeException {

    public InvalidProductVariantException(String message) {
        super(message);
    }
}
