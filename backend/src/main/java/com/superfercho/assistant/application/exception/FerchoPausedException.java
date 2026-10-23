package com.superfercho.assistant.application.exception;

/**
 * Fercho está temporalmente no disponible para el cliente por una restricción
 * vigente (pausa automática por límite o suspensión administrativa).
 */
public class FerchoPausedException extends RuntimeException {

    private final String code;
    private final long remainingSeconds;
    private final java.time.Instant pausedUntil;

    public FerchoPausedException(String code, String message, long remainingSeconds, java.time.Instant pausedUntil) {
        super(message);
        this.code = code;
        this.remainingSeconds = remainingSeconds;
        this.pausedUntil = pausedUntil;
    }

    public String code() {
        return code;
    }

    public long remainingSeconds() {
        return remainingSeconds;
    }

    public java.time.Instant pausedUntil() {
        return pausedUntil;
    }
}
