package com.superfercho.identity.application.exception;

public class PasswordRecoveryRateLimitedException extends RuntimeException {

    public PasswordRecoveryRateLimitedException() {
        super("Too many password recovery requests. Try again later.");
    }
}
