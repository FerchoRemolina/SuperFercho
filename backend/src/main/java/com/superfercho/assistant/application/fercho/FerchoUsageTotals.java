package com.superfercho.assistant.application.fercho;

import java.math.BigDecimal;
import java.time.Instant;

public record FerchoUsageTotals(
        Instant windowStart,
        Instant windowEnd,
        long acceptedRequests,
        long rejectedRequests,
        long llmCalls,
        Long promptTokens,
        Long completionTokens,
        BigDecimal estimatedCost
) {
}
