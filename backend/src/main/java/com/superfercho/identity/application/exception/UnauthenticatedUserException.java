package com.superfercho.identity.application.exception;

public class UnauthenticatedUserException extends RuntimeException {

    public UnauthenticatedUserException() {
        super("Authenticated user is required");
    }
}
