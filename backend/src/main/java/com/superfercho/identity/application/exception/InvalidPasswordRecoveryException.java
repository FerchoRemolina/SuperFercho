package com.superfercho.identity.application.exception;

public class InvalidPasswordRecoveryException extends RuntimeException {

    public InvalidPasswordRecoveryException(String message) {
        super(message);
    }

    public static InvalidPasswordRecoveryException invalidToken() {
        return new InvalidPasswordRecoveryException("El enlace de recuperación no es válido o ya expiró.");
    }

    public static InvalidPasswordRecoveryException passwordMismatch() {
        return new InvalidPasswordRecoveryException("Las contraseñas no coinciden.");
    }
}
