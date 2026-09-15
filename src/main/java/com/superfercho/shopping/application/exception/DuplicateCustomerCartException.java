package com.superfercho.shopping.application.exception;

public class DuplicateCustomerCartException extends RuntimeException {

    public DuplicateCustomerCartException() {
        super("A cart already exists for this customer");
    }
}
