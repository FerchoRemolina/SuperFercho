package com.superfercho.assistant.application.fercho;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Control atómico de límites de Fercho: reserva del turno dentro de la ventana
 * móvil, creación de pausas y métricas de uso. Falla cerrado: si la
 * persistencia falla, el método lanza {@link FerchoUsageLedgerUnavailableException}
 * y el endpoint de chat devuelve un error controlado sin invocar la IA.
 */
public interface FerchoUsageGate {

    /**
     * Evalúa y reserva el turno. Operación atómica por usuario (advisory lock):
     * dos solicitudes concurrentes no pueden superar el límite.
     */
    FerchoTurnDecision evaluateAndReserve(UUID userId, FerchoLimits limits, Instant now);

    /** Completa el turno aceptado con las métricas reales de IA y coste. */
    void completeAcceptedTurn(
            UUID turnId,
            int llmCalls,
            Integer promptTokens,
            Integer completionTokens,
            String model,
            BigDecimal estimatedCost,
            Instant now);

    /** Registra un turno rechazado por restricción (métrica separada). */
    void recordRejectedTurn(UUID userId, FerchoTurnDecisionType rejectionType, String reason, Instant now);
}
