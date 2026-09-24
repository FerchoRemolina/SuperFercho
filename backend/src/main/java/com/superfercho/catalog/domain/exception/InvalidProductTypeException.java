package com.superfercho.catalog.domain.exception;

public class InvalidProductTypeException extends RuntimeException {

    public InvalidProductTypeException(String message) {
        super(message);
    }
}
