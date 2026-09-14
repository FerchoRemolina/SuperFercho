package com.superfercho.identity.application.exception;

public class DocumentAlreadyExistsException extends RuntimeException {

    public DocumentAlreadyExistsException() {
        super("A user with this document already exists");
    }

    public DocumentAlreadyExistsException(String documentType, String documentNumber) {
        super("A user with document already exists: " + documentType + " " + documentNumber);
    }
}
