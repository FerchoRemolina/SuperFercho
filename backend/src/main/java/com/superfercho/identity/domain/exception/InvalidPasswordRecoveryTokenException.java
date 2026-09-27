package com.superfercho.identity.domain.exception;

public class InvalidPasswordRecoveryTokenException extends RuntimeException {

    public InvalidPasswordRecoveryTokenException(String message) {
        super(message);
    }
}
