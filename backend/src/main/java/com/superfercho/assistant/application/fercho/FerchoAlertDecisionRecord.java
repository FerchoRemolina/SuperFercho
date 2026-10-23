package com.superfercho.assistant.application.fercho;

import java.time.Instant;
import java.util.UUID;

public record FerchoAlertDecisionRecord(
        UUID alertId,
        FerchoAlertDecision decision,
        UUID adminId,
        String note,
        Instant decidedAt,
        UUID suspensionId,
        Instant suspensionExpiresAt
) {
}
