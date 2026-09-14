package com.superfercho.identity.application.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("A user with email already exists: " + email);
    }
}
