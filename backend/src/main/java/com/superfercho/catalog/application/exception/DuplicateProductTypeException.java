package com.superfercho.catalog.application.exception;

public class DuplicateProductTypeException extends RuntimeException {

    public DuplicateProductTypeException() {
        super("A product type with this name already exists in the category");
    }
}
