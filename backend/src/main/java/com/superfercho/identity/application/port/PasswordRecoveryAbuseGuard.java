package com.superfercho.identity.application.port;

/**
 * Lightweight abuse protection for public recovery endpoints (e.g. per-IP window).
 * Not a full gateway rate-limiter; keeps Identity freestanding.
 */
public interface PasswordRecoveryAbuseGuard {

    boolean allowRequest(String clientKey);
}
