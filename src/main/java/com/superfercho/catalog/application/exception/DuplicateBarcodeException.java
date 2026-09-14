package com.superfercho.catalog.application.exception;

public class DuplicateBarcodeException extends RuntimeException {

    public DuplicateBarcodeException() {
        super("A product with this barcode already exists");
    }
}
