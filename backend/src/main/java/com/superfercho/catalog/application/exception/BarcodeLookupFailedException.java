package com.superfercho.catalog.application.exception;

public final class BarcodeLookupFailedException extends RuntimeException {

    public BarcodeLookupFailedException(String message) {
        super(message);
    }

    public BarcodeLookupFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
