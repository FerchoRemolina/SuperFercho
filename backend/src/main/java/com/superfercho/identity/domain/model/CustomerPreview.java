package com.superfercho.identity.domain.model;

import com.superfercho.identity.domain.exception.InvalidCustomerPreviewException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class CustomerPreview {

    public static final Duration LIFETIME = Duration.ofMinutes(20);

    private final UUID id;
    private final UUID adminUserId;
    private final UUID temporaryCustomerId;
    private final CustomerPreviewStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant closedAt;

    private CustomerPreview(
            UUID id,
            UUID adminUserId,
            UUID temporaryCustomerId,
            CustomerPreviewStatus status,
            Instant createdAt,
            Instant expiresAt,
            Instant closedAt) {
        this.id = id;
        this.adminUserId = adminUserId;
        this.temporaryCustomerId = temporaryCustomerId;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.closedAt = closedAt;
    }

    public static CustomerPreview start(
            UUID id, UUID adminUserId, UUID temporaryCustomerId, Instant createdAt) {
        requireNonNull(id, "id");
        requireNonNull(adminUserId, "adminUserId");
        requireNonNull(temporaryCustomerId, "temporaryCustomerId");
        requireNonNull(createdAt, "createdAt");
        if (adminUserId.equals(temporaryCustomerId)) {
            throw new InvalidCustomerPreviewException(
                    "temporaryCustomerId must differ from adminUserId");
        }
        return new CustomerPreview(
                id,
                adminUserId,
                temporaryCustomerId,
                CustomerPreviewStatus.ACTIVE,
                createdAt,
                createdAt.plus(LIFETIME),
                null);
    }

    public static CustomerPreview reconstitute(
            UUID id,
            UUID adminUserId,
            UUID temporaryCustomerId,
            CustomerPreviewStatus status,
            Instant createdAt,
            Instant expiresAt,
            Instant closedAt) {
        requireNonNull(id, "id");
        requireNonNull(adminUserId, "adminUserId");
        requireNonNull(temporaryCustomerId, "temporaryCustomerId");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new InvalidCustomerPreviewException("expiresAt must be after createdAt");
        }
        if (status == CustomerPreviewStatus.CLOSED && closedAt == null) {
            throw new InvalidCustomerPreviewException("closed preview requires closedAt");
        }
        if (status == CustomerPreviewStatus.ACTIVE && closedAt != null) {
            throw new InvalidCustomerPreviewException("active preview cannot have closedAt");
        }
        return new CustomerPreview(
                id, adminUserId, temporaryCustomerId, status, createdAt, expiresAt, closedAt);
    }

    public boolean isExpired(Instant now) {
        requireNonNull(now, "now");
        return !now.isBefore(expiresAt);
    }

    public boolean isUsable(Instant now) {
        return status == CustomerPreviewStatus.ACTIVE && !isExpired(now);
    }

    public CustomerPreview close(Instant closedAt) {
        requireNonNull(closedAt, "closedAt");
        if (status == CustomerPreviewStatus.CLOSED) {
            return this;
        }
        return new CustomerPreview(
                id,
                adminUserId,
                temporaryCustomerId,
                CustomerPreviewStatus.CLOSED,
                createdAt,
                expiresAt,
                closedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID adminUserId() {
        return adminUserId;
    }

    public UUID temporaryCustomerId() {
        return temporaryCustomerId;
    }

    public CustomerPreviewStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    private static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new InvalidCustomerPreviewException(name + " is required");
        }
    }
}
