package com.superfercho.shopping.application.exception;

public class DuplicateCartAddOperationException extends RuntimeException {

    public DuplicateCartAddOperationException() {
        super("A cart add operation with this id already exists");
    }
}
