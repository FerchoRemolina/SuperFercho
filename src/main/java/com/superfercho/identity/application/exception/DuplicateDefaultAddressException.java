package com.superfercho.identity.application.exception;

public class DuplicateDefaultAddressException extends RuntimeException {

    public DuplicateDefaultAddressException() {
        super("A user can have at most one active default address");
    }
}
