package com.superfercho.identity.application.exception;

public class InactiveUserException extends RuntimeException {

    public InactiveUserException() {
        super("User is inactive");
    }
}
