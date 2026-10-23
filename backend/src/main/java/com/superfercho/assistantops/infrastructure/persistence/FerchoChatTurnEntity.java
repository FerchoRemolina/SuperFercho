package com.superfercho.assistantops.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fercho_chat_turns", schema = "assistant")
public class FerchoChatTurnEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "llm_calls", nullable = false)
    private int llmCalls;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "estimated_cost", precision = 12, scale = 6)
    private java.math.BigDecimal estimatedCost;

    @Column(length = 64)
    private String model;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FerchoChatTurnEntity() {
    }

    public FerchoChatTurnEntity(
            UUID id, UUID userId, String status, int llmCalls, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.llmCalls = llmCalls;
        this.createdAt = createdAt;
    }

    public void recordUsage(
            int llmCalls,
            Integer promptTokens,
            Integer completionTokens,
            java.math.BigDecimal estimatedCost,
            String model) {
        this.llmCalls = llmCalls;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.estimatedCost = estimatedCost;
        this.model = model;
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

    public int getLlmCalls() {
        return llmCalls;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public java.math.BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public String getModel() {
        return model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
