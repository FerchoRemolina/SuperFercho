package com.superfercho.assistantops.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fercho_admin_alerts", schema = "assistant")
public class FerchoAdminAlertEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "accepted_requests", nullable = false)
    private int acceptedRequests;

    @Column(name = "llm_calls", nullable = false)
    private int llmCalls;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "estimated_cost", precision = 12, scale = 6)
    private BigDecimal estimatedCost;

    @Column(length = 64)
    private String model;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decision_note", length = 300)
    private String decisionNote;

    @Column(name = "restriction_id")
    private UUID restrictionId;

    protected FerchoAdminAlertEntity() {
    }

    public FerchoAdminAlertEntity(
            UUID id,
            UUID userId,
            String status,
            String reason,
            Instant windowStartedAt,
            int acceptedRequests,
            int llmCalls,
            Instant createdAt,
            UUID restrictionId) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.reason = reason;
        this.windowStartedAt = windowStartedAt;
        this.acceptedRequests = acceptedRequests;
        this.llmCalls = llmCalls;
        this.createdAt = createdAt;
        this.restrictionId = restrictionId;
    }

    public void decide(
            String newStatus, UUID by, String note, Instant at, BigDecimal cost, Integer completionTokens, String model) {
        this.status = newStatus;
        this.decidedAt = at;
        this.decidedBy = by;
        this.decisionNote = note;
        if (cost != null) {
            this.estimatedCost = cost;
        }
        if (completionTokens != null) {
            this.completionTokens = completionTokens;
        }
        if (model != null) {
            this.model = model;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Instant getWindowStartedAt() {
        return windowStartedAt;
    }

    public int getAcceptedRequests() {
        return acceptedRequests;
    }

    public int getLlmCalls() {
        return llmCalls;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public String getModel() {
        return model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public UUID getRestrictionId() {
        return restrictionId;
    }

    public void setRestrictionId(UUID restrictionId) {
        this.restrictionId = restrictionId;
    }
}
