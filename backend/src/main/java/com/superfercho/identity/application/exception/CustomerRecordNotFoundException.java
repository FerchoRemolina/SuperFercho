package com.superfercho.identity.application.exception;

import java.util.UUID;

public class CustomerRecordNotFoundException extends RuntimeException {

    public CustomerRecordNotFoundException(UUID customerRecordId) {
        super("CustomerRecord not found: " + customerRecordId);
    }

    public CustomerRecordNotFoundException(String documentType, String documentNumber) {
        super("CustomerRecord not found for document: " + documentType + " " + documentNumber);
    }
}
