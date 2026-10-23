package com.superfercho.assistant.application.fercho;

import java.time.Instant;
import java.util.UUID;

public record FerchoRestrictionRecord(
        UUID id,
        UUID userId,
        String type,
        String reason,
        Instant createdAt,
        UUID createdBy,
        Instant expiresAt,
        Instant liftedAt,
        UUID liftedBy,
        String note
) {
}
