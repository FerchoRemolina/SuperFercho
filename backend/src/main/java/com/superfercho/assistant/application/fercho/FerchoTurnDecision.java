package com.superfercho.assistant.application.fercho;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Decisión atómica del control de uso de Fercho para un turno solicitado.
 *
 * @param type        ACCEPTED: continúa a la IA. PAUSED: pausa automática por
 *                    límite. SUSPENDED: suspensión administrativa vigente.
 * @param turnId      identificador del turno registrado (aceptado o rechazado).
 * @param pausedUntil vencimiento real según el servidor (solo en PAUSED y
 *                    SUSPENDED).
 * @param reason      motivo determinista (LIMIT_EXCEEDED, ADMIN_DECISION,
 *                    PERSISTENCE_FAILURE).
 * @param estimatedCostSnapshot costo estimado acumulado de la ventana cuando el
 *                              proveedor entregó tokens; null si no se puede
 *                              calcular.
 */
public record FerchoTurnDecision(
        FerchoTurnDecisionType type,
        java.util.UUID turnId,
        java.util.UUID userId,
        Instant pausedUntil,
        String reason,
        BigDecimal estimatedCostSnapshot,
        Duration remainingPause
) {

    public static FerchoTurnDecision accepted(java.util.UUID turnId) {
        return new FerchoTurnDecision(FerchoTurnDecisionType.ACCEPTED, turnId, null, null, null, null, null);
    }

    public boolean accepted() {
        return type == FerchoTurnDecisionType.ACCEPTED;
    }
}
