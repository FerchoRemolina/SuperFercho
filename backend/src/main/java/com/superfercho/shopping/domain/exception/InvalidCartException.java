package com.superfercho.shopping.domain.exception;

public class InvalidCartException extends RuntimeException {

    public InvalidCartException(String message) {
        super(message);
    }
}
