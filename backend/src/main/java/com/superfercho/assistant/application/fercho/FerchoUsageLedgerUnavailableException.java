package com.superfercho.assistant.application.fercho;

/** La persistencia del control de uso no está disponible (fail-closed). */
public class FerchoUsageLedgerUnavailableException extends RuntimeException {

    public FerchoUsageLedgerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
