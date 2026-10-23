package com.superfercho.assistantops.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fercho_restrictions", schema = "assistant")
public class FerchoRestrictionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 24)
    private String type;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "lifted_at")
    private Instant liftedAt;

    @Column(name = "lifted_by")
    private UUID liftedBy;

    @Column(length = 300)
    private String note;

    protected FerchoRestrictionEntity() {
    }

    public FerchoRestrictionEntity(
            UUID id,
            UUID userId,
            String type,
            String reason,
            UUID createdBy,
            Instant expiresAt,
            String note,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.reason = reason;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
        this.note = note;
        this.createdAt = createdAt;
    }

    public boolean activeAt(Instant now) {
        return liftedAt == null && expiresAt.isAfter(now);
    }

    public void lift(UUID by, Instant at) {
        this.liftedAt = at;
        this.liftedBy = by;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLiftedAt() {
        return liftedAt;
    }

    public UUID getLiftedBy() {
        return liftedBy;
    }

    public String getNote() {
        return note;
    }
}
