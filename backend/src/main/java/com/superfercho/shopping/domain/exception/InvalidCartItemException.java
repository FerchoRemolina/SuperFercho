package com.superfercho.shopping.domain.exception;

public class InvalidCartItemException extends RuntimeException {

    public InvalidCartItemException(String message) {
        super(message);
    }
}
