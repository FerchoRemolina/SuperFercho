package com.superfercho.catalog.application.exception;

public final class BarcodeLookupNotFoundException extends RuntimeException {

    public BarcodeLookupNotFoundException(String barcode) {
        super("No product found for barcode: " + barcode);
    }
}
