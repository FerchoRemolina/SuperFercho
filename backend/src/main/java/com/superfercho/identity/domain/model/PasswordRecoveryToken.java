package com.superfercho.identity.domain.model;

import com.superfercho.identity.domain.exception.InvalidPasswordRecoveryTokenException;
import java.time.Instant;
import java.util.UUID;

public final class PasswordRecoveryToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final String requestIp;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant consumedAt;

    private PasswordRecoveryToken(
            UUID id,
            UUID userId,
            String tokenHash,
            String requestIp,
            Instant createdAt,
            Instant expiresAt,
            Instant consumedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.requestIp = requestIp;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
    }

    public static PasswordRecoveryToken create(
            UUID id,
            UUID userId,
            String tokenHash,
            String requestIp,
            Instant createdAt,
            Instant expiresAt) {
        requireNonNull(id, "id");
        requireNonNull(userId, "userId");
        requireText(tokenHash, "tokenHash");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new InvalidPasswordRecoveryTokenException("expiresAt must be after createdAt");
        }
        return new PasswordRecoveryToken(id, userId, tokenHash, requestIp, createdAt, expiresAt, null);
    }

    public static PasswordRecoveryToken restore(
            UUID id,
            UUID userId,
            String tokenHash,
            String requestIp,
            Instant createdAt,
            Instant expiresAt,
            Instant consumedAt) {
        requireNonNull(id, "id");
        requireNonNull(userId, "userId");
        requireText(tokenHash, "tokenHash");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(expiresAt, "expiresAt");
        return new PasswordRecoveryToken(
                id, userId, tokenHash, requestIp, createdAt, expiresAt, consumedAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isExpired(Instant now) {
        requireNonNull(now, "now");
        return !expiresAt.isAfter(now);
    }

    public boolean isUsable(Instant now) {
        return !isConsumed() && !isExpired(now);
    }

    public PasswordRecoveryToken consume(Instant consumedAt) {
        requireNonNull(consumedAt, "consumedAt");
        if (isConsumed()) {
            throw new InvalidPasswordRecoveryTokenException("token already consumed");
        }
        return new PasswordRecoveryToken(
                id, userId, tokenHash, requestIp, createdAt, expiresAt, consumedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public String requestIp() {
        return requestIp;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant consumedAt() {
        return consumedAt;
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidPasswordRecoveryTokenException(field + " cannot be null");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidPasswordRecoveryTokenException(field + " cannot be null or blank");
        }
    }
}
