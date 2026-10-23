package com.superfercho.assistant.application.fercho;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FerchoActiveRestriction(
        UUID restrictionId,
        UUID userId,
        String type,
        String reason,
        Instant expiresAt,
        Instant createdAt
) {
}
