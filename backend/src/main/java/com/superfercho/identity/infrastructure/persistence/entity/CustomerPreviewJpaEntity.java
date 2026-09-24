package com.superfercho.identity.infrastructure.persistence.entity;

import com.superfercho.identity.domain.model.CustomerPreviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_previews", schema = "identity")
public class CustomerPreviewJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "admin_user_id", nullable = false)
    private UUID adminUserId;

    @Column(name = "temporary_customer_id", nullable = false)
    private UUID temporaryCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerPreviewStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected CustomerPreviewJpaEntity() {}

    public CustomerPreviewJpaEntity(
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

    public UUID getId() {
        return id;
    }

    public UUID getAdminUserId() {
        return adminUserId;
    }

    public UUID getTemporaryCustomerId() {
        return temporaryCustomerId;
    }

    public CustomerPreviewStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setStatus(CustomerPreviewStatus status) {
        this.status = status;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}
