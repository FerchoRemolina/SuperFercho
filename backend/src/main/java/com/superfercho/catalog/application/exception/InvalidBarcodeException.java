package com.superfercho.catalog.application.exception;

public final class InvalidBarcodeException extends RuntimeException {

    public InvalidBarcodeException(String message) {
        super(message);
    }
}
