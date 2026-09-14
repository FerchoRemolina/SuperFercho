package com.superfercho.catalog.application.exception;

import java.util.UUID;

public class InvalidCategoryReferenceException extends RuntimeException {

    public InvalidCategoryReferenceException() {
        super("Invalid category reference");
    }

    public InvalidCategoryReferenceException(UUID categoryId) {
        super("Invalid category reference: " + categoryId);
    }
}
