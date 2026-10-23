package com.superfercho.assistant.application.fercho;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FerchoClientUsageSummary(
        UUID userId,
        Instant windowStart,
        Instant windowEnd,
        int acceptedRequests,
        int rejectedRequests,
        int llmCalls,
        Integer promptTokens,
        Integer completionTokens,
        BigDecimal estimatedCost,
        String model,
        UUID activeRestrictionId,
        String activeRestrictionType,
        Instant activeRestrictionExpiresAt
) {
}
