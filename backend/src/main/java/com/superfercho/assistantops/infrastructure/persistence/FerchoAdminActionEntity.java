package com.superfercho.assistantops.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fercho_admin_actions", schema = "assistant")
public class FerchoAdminActionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(length = 300)
    private String note;

    @Column(name = "restriction_id")
    private UUID restrictionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FerchoAdminActionEntity() {
    }

    public FerchoAdminActionEntity(
            UUID id,
            UUID alertId,
            UUID targetUserId,
            String action,
            UUID adminId,
            String note,
            UUID restrictionId,
            Instant createdAt) {
        this.id = id;
        this.alertId = alertId;
        this.targetUserId = targetUserId;
        this.action = action;
        this.adminId = adminId;
        this.note = note;
        this.restrictionId = restrictionId;
        this.createdAt = createdAt;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public String getAction() {
        return action;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public String getNote() {
        return note;
    }

    public UUID getRestrictionId() {
        return restrictionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
