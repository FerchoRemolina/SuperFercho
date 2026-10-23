package com.superfercho.assistant.application.fercho;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FerchoAlertRecord(
        UUID id,
        UUID userId,
        String status,
        String reason,
        Instant windowStartedAt,
        int acceptedRequests,
        int llmCalls,
        Integer promptTokens,
        Integer completionTokens,
        BigDecimal estimatedCost,
        String model,
        Instant createdAt,
        Instant decidedAt,
        UUID decidedBy,
        String decisionNote,
        UUID restrictionId
) {
}
