package com.superfercho.identity.application.dto;

/**
 * Always the same public message — never reveals whether the email exists.
 */
public record PasswordRecoveryRequestResult(String message) {

    public static final String GENERIC_MESSAGE =
            "Si existe una cuenta asociada a este correo, recibirás un enlace para restablecer tu contraseña.";

    public static PasswordRecoveryRequestResult generic() {
        return new PasswordRecoveryRequestResult(GENERIC_MESSAGE);
    }
}
