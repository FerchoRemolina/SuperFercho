package com.superfercho.identity.application.exception;

import java.util.UUID;

public class InactiveUserException extends RuntimeException {

    public InactiveUserException(UUID userId) {
        super("User is inactive: " + userId);
    }
}
