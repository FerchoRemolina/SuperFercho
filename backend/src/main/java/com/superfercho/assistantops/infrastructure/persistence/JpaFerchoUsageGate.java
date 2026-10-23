package com.superfercho.assistantops.infrastructure.persistence;

import com.superfercho.assistant.application.exception.FerchoPausedException;
import com.superfercho.assistant.application.fercho.FerchoUsageLedgerUnavailableException;
import com.superfercho.assistant.application.fercho.FerchoLimits;
import com.superfercho.assistant.application.fercho.FerchoTurnDecision;
import com.superfercho.assistant.application.fercho.FerchoTurnDecisionType;
import com.superfercho.assistant.application.fercho.FerchoUsageGate;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Control atómico de límites de Fercho sobre PostgreSQL. Fail-closed: un fallo
 * de persistencia lanza {@link FerchoPausedException} (FERCHO_UNAVAILABLE) y el
 * chat no invoca la IA; el resto de SuperFercho sigue funcionando.
 */
@Component
@Profile("!test")
public class JpaFerchoUsageGate implements FerchoUsageGate {

    public static final String TURN_ACCEPTED = "ACCEPTED";
    public static final String TURN_REJECTED_LIMIT = "REJECTED_LIMIT";
    public static final String TURN_REJECTED_PAUSED = "REJECTED_PAUSED";
    public static final String TURN_REJECTED_SUSPENDED = "REJECTED_SUSPENDED";
    public static final String TURN_REJECTED_UNAVAILABLE = "REJECTED_UNAVAILABLE";

    private final FerchoChatTurnJpaRepository turns;
    private final FerchoRestrictionJpaRepository restrictions;
    private final FerchoAdminAlertJpaRepository alerts;

    public JpaFerchoUsageGate(
            FerchoChatTurnJpaRepository turns,
            FerchoRestrictionJpaRepository restrictions,
            FerchoAdminAlertJpaRepository alerts) {
        this.turns = turns;
        this.restrictions = restrictions;
        this.alerts = alerts;
    }

    @Override
    @Transactional
    public FerchoTurnDecision evaluateAndReserve(UUID userId, FerchoLimits limits, Instant now) {
        try {
            turns.acquireUserLock(userId.toString());
            var active = restrictions
                    .findFirstByUserIdAndLiftedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(userId, now);
            if (active.isPresent()) {
                var restriction = active.get();
                boolean suspended = "ADMIN_SUSPENSION".equals(restriction.getType());
                String turnStatus = suspended ? TURN_REJECTED_SUSPENDED : TURN_REJECTED_PAUSED;
                turns.save(new FerchoChatTurnEntity(
                        UUID.randomUUID(), userId, turnStatus, 0, now));
                return new FerchoTurnDecision(
                        suspended ? FerchoTurnDecisionType.SUSPENDED : FerchoTurnDecisionType.PAUSED,
                        null,
                        userId,
                        restriction.getExpiresAt(),
                        suspended ? "ADMIN_DECISION" : "LIMIT_EXCEEDED",
                        null,
                        java.time.Duration.between(now, restriction.getExpiresAt()));
            }
            var windowStart = now.minus(limits.window());
            long acceptedInWindow =
                    turns.countByUserIdAndStatusAndCreatedAtAfter(userId, TURN_ACCEPTED, windowStart);
            if (acceptedInWindow < limits.maxRequestsPerWindow()) {
                var turnId = UUID.randomUUID();
                turns.save(new FerchoChatTurnEntity(turnId, userId, TURN_ACCEPTED, 0, now));
                return FerchoTurnDecision.accepted(turnId);
            }
            // Límite alcanzado: pausa automática de 30 min (configurable).
            var restrictionId = UUID.randomUUID();
            var expiresAt = now.plus(limits.pauseDuration());
            restrictions.save(new FerchoRestrictionEntity(
                    restrictionId,
                    userId,
                    "AUTO_PAUSE",
                    "LIMIT_EXCEEDED",
                    null,
                    expiresAt,
                    "Límite de " + limits.maxRequestsPerWindow() + " solicitudes en "
                            + limits.window().toHours() + " h alcanzado.",
                    now));
            turns.save(new FerchoChatTurnEntity(UUID.randomUUID(), userId, TURN_REJECTED_LIMIT, 0, now));
            raiseRecidiveAlertIfNeeded(userId, limits, now, windowStart, restrictionId);
            return new FerchoTurnDecision(
                    FerchoTurnDecisionType.PAUSED,
                    null,
                    userId,
                    expiresAt,
                    "LIMIT_EXCEEDED",
                    null,
                    limits.pauseDuration());
        } catch (DataAccessException exception) {
            // Fail-closed: no se puede evaluar el límite → Fercho no responde.
            throw new FerchoUsageLedgerUnavailableException(
                    "fercho usage ledger is unavailable", exception);
        }
    }

    /**
     * Reincidencia: existe una pausa previa del mismo cliente dentro de la
     * ventana de reincidencia. La alerta se genera una sola vez por episodio
     * (índice único parcial sobre alertas OPEN).
     */
    private void raiseRecidiveAlertIfNeeded(
            UUID userId, FerchoLimits limits, Instant now, Instant windowStart, UUID restrictionId) {
        boolean recidive = restrictions.existsByUserIdAndTypeAndCreatedAtAfterAndIdNot(
                userId, "AUTO_PAUSE", now.minus(limits.recidiveWindow()), restrictionId);
        if (!recidive || alerts.existsByUserIdAndStatus(userId, "OPEN")) {
            return;
        }
        var totals = turns.sumWindowForUser(userId, windowStart, now);
        long accepted = ((Number) totals.get(0)[0]).longValue();
        long llmCalls = ((Number) totals.get(0)[1]).longValue();
        var alert = new FerchoAdminAlertEntity(
                UUID.randomUUID(),
                userId,
                "OPEN",
                "RECIDIVE_LIMIT_EXCEEDED",
                windowStart,
                (int) accepted,
                (int) llmCalls,
                now,
                restrictionId);
        alerts.save(alert);
    }

    @Override
    @Transactional
    public void completeAcceptedTurn(
            UUID turnId,
            int llmCalls,
            Integer promptTokens,
            Integer completionTokens,
            String model,
            java.math.BigDecimal estimatedCost,
            Instant now) {
        turns.findById(turnId).ifPresent(turn -> turn.recordUsage(
                llmCalls,
                promptTokens,
                completionTokens,
                estimatedCost,
                // El costo estimado lo calcula la supervisión con las tarifas
                // vigentes; aquí solo se persisten los tokens reales.
                model));
    }

    @Override
    @Transactional
    public void recordRejectedTurn(
            UUID userId, FerchoTurnDecisionType rejectionType, String reason, Instant now) {
        String status = switch (rejectionType) {
            case PAUSED -> TURN_REJECTED_PAUSED;
            case SUSPENDED -> TURN_REJECTED_SUSPENDED;
            default -> TURN_REJECTED_UNAVAILABLE;
        };
        turns.save(new FerchoChatTurnEntity(UUID.randomUUID(), userId, status, 0, now));
    }
}
