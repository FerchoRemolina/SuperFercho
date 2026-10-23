package com.superfercho.assistantops.infrastructure.persistence;

import com.superfercho.assistant.application.fercho.FerchoAlertDecision;
import com.superfercho.assistant.application.fercho.FerchoAlertRecord;
import com.superfercho.assistant.application.fercho.FerchoClientUsageSummary;
import com.superfercho.assistant.application.fercho.FerchoRestrictionRecord;
import com.superfercho.assistant.application.fercho.FerchoSupervisionPort;
import com.superfercho.assistant.application.fercho.FerchoUsageTotals;
import com.superfercho.assistant.infrastructure.configuration.FerchoCostProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supervisión administrativa de Fercho sobre PostgreSQL: alertas,
 * restricciones, métricas agregadas y auditoría de decisiones.
 *
 * El costo estimado se calcula en consulta con las tarifas vigentes
 * (metodología: tokens/1000 × tarifa); los históricos pueden variar si
 * cambian las tarifas. Sin tarifas configuradas, el costo es null.
 */
@Component
@Profile("!test")
public class JpaFerchoSupervisionAdapter implements FerchoSupervisionPort {

    private final FerchoChatTurnJpaRepository turns;
    private final FerchoRestrictionJpaRepository restrictions;
    private final FerchoAdminAlertJpaRepository alerts;
    private final FerchoAdminActionJpaRepository actions;
    private final FerchoCostProperties costProperties;

    public JpaFerchoSupervisionAdapter(
            FerchoChatTurnJpaRepository turns,
            FerchoRestrictionJpaRepository restrictions,
            FerchoAdminAlertJpaRepository alerts,
            FerchoAdminActionJpaRepository actions,
            FerchoCostProperties costProperties) {
        this.turns = turns;
        this.restrictions = restrictions;
        this.alerts = alerts;
        this.actions = actions;
        this.costProperties = costProperties;
    }

    @Override
    public List<FerchoAlertRecord> findAlerts(String statusOrNull, int limit) {
        var entities = statusOrNull == null || statusOrNull.isBlank()
                ? alerts.findTop50ByOrderByCreatedAtDesc()
                : alerts.findByStatusOrderByCreatedAtDesc(statusOrNull);
        return entities.stream().limit(limit).map(this::toAlertRecord).toList();
    }

    @Override
    public Optional<FerchoAlertRecord> findAlert(UUID alertId) {
        return alerts.findById(alertId).map(this::toAlertRecord);
    }

    @Override
    public List<FerchoRestrictionRecord> findActiveRestrictions(int limit) {
        return restrictions
                .findByLiftedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(Instant.now())
                .stream()
                .limit(limit)
                .map(this::toRestrictionRecord)
                .toList();
    }

    @Override
    public List<FerchoRestrictionRecord> findRestrictionsForUser(UUID userId, int limit) {
        return restrictions.findTop12ByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(limit)
                .map(this::toRestrictionRecord)
                .toList();
    }

    @Override
    public Optional<FerchoRestrictionRecord> findRestriction(UUID restrictionId) {
        return restrictions.findById(restrictionId).map(this::toRestrictionRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public FerchoClientUsageSummary clientUsage(UUID userId, Instant windowStart, Instant windowEnd) {
        long accepted = turns.countByUserIdAndStatusAndCreatedAtBetween(
                userId, "ACCEPTED", windowStart, windowEnd);
        long rejected = turns.countByUserIdAndStatusStartingWithAndCreatedAtBetween(
                userId, "REJECTED", windowStart, windowEnd);
        var rows = sumWindowForUser(userId, windowStart, windowEnd);
        Object[] sums = rows.isEmpty() ? new Object[]{0L, 0L, 0L} : rows.get(0);
        long llmCalls = asLong(sums[0]);
        long prompt = asLong(sums[1]);
        long completion = asLong(sums[2]);
        var active = restrictions
                .findFirstByUserIdAndLiftedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(userId, Instant.now());
        return new FerchoClientUsageSummary(
                userId,
                windowStart,
                windowEnd,
                (int) Math.min(accepted, Integer.MAX_VALUE),
                (int) Math.min(rejected, Integer.MAX_VALUE),
                (int) Math.min(llmCalls, Integer.MAX_VALUE),
                (int) Math.min(prompt, Integer.MAX_VALUE),
                (int) Math.min(completion, Integer.MAX_VALUE),
                estimatedCost(prompt, completion),
                costProperties.model(),
                active.map(FerchoRestrictionEntity::getId).orElse(null),
                active.map(FerchoRestrictionEntity::getType).orElse(null),
                active.map(FerchoRestrictionEntity::getExpiresAt).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public FerchoUsageTotals totals(Instant windowStart, Instant windowEnd) {
        long accepted = turns.countByStatusAndCreatedAtBetween("ACCEPTED", windowStart, windowEnd);
        long rejected = turns.countByStatusStartingWithAndCreatedAtBetween("REJECTED", windowStart, windowEnd);
        Object[] sums = sumWindow(null, windowStart, windowEnd);
        long llmCalls = asLong(sums[0]);
        long prompt = asLong(sums[1]);
        long completion = asLong(sums[2]);
        return new FerchoUsageTotals(
                windowStart,
                windowEnd,
                accepted,
                rejected,
                (int) Math.min(llmCalls, Integer.MAX_VALUE),
                prompt == 0 ? null : Long.valueOf(Math.min(prompt, Integer.MAX_VALUE)),
                completion == 0 ? null : Long.valueOf(Math.min(completion, Integer.MAX_VALUE)),
                estimatedCost(prompt, completion));
    }

    @Override
    public int countActiveAutoPauses(Instant now) {
        return (int) restrictions
                .countByTypeAndLiftedAtIsNullAndExpiresAtAfter("AUTO_PAUSE", now);
    }

    @Override
    public int countActiveSuspensions(Instant now) {
        return (int) restrictions
                .countByTypeAndLiftedAtIsNullAndExpiresAtAfter("ADMIN_SUSPENSION", now);
    }

    @Override
    public int countOpenAlerts() {
        return (int) alerts.countByStatus("OPEN");
    }

    /**
     * Protegido contra doble ejecución: una alerta que ya no está OPEN no se
     * vuelve a decidir (devuelve null). Toda la operación es transaccional:
     * alerta + suspensión + auditoría se confirman o revierten juntas.
     */
    @Override
    @Transactional
    public UUID resolveAlert(
            UUID alertId,
            FerchoAlertDecision decision,
            UUID adminId,
            String note,
            java.time.Duration suspensionDuration,
            Instant now) {
        var alert = alerts.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("alert not found: " + alertId));
        if (!"OPEN".equals(alert.getStatus())) {
            return null;
        }
        String persistedStatus = switch (decision) {
            case DISMISS -> "DISMISSED";
            case WARN -> "WARNED";
            case SUSPEND -> "SUSPENDED";
        };
        UUID restrictionId = null;
        if (decision == FerchoAlertDecision.SUSPEND) {
            restrictionId = UUID.randomUUID();
            restrictions.save(new FerchoRestrictionEntity(
                    restrictionId,
                    alert.getUserId(),
                    "ADMIN_SUSPENSION",
                    "ADMIN_DECISION",
                    adminId,
                    now.plus(suspensionDuration),
                    note,
                    now));
        }
        alert.decide(persistedStatus, adminId, note, now, null, null, null);
        if (restrictionId != null) {
            alert.setRestrictionId(restrictionId);
        }
        alerts.save(alert);
        actions.save(new FerchoAdminActionEntity(
                UUID.randomUUID(),
                alertId,
                alert.getUserId(),
                decision.name(),
                adminId,
                note,
                restrictionId,
                now));
        return restrictionId;
    }

    @Override
    @Transactional
    public void liftSuspension(UUID restrictionId, UUID adminId, String note, Instant now) {
        var restriction = restrictions.findById(restrictionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "restriction not found: " + restrictionId));
        if (restriction.getLiftedAt() != null) {
            return;
        }
        restriction.lift(adminId, now);
        restrictions.save(restriction);
        actions.save(new FerchoAdminActionEntity(
                UUID.randomUUID(),
                null,
                restriction.getUserId(),
                "LIFT_SUSPENSION",
                adminId,
                note,
                restrictionId,
                now));
    }

    /** Consulta la lista de turnos del cliente dentro de la ventana. */
    private List<Object[]> sumWindowForUser(UUID userId, Instant start, Instant end) {
        return turns.sumWindowForUser(userId, start, end);
    }

    /** Agregado global (sin filtro de usuario) de turnos en la ventana. */
    private Object[] sumWindow(UUID ignore, Instant start, Instant end) {
        var rows = turns.sumWindowGlobal(start, end);
        return rows.isEmpty() ? new Object[]{0L, 0L, 0L} : rows.get(0);
    }

    private static long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private java.math.BigDecimal estimatedCost(long promptTokens, long completionTokens) {
        if (!costProperties.calculable()) {
            return null;
        }
        var prompt = java.math.BigDecimal.valueOf(promptTokens)
                .divide(java.math.BigDecimal.valueOf(1000))
                .multiply(costProperties.promptPer1k());
        var completion = java.math.BigDecimal.valueOf(completionTokens)
                .divide(java.math.BigDecimal.valueOf(1000))
                .multiply(costProperties.completionPer1k());
        return prompt.add(completion).setScale(6, RoundingMode.HALF_UP);
    }

    private FerchoAlertRecord toAlertRecord(FerchoAdminAlertEntity a) {
        return new FerchoAlertRecord(
                a.getId(),
                a.getUserId(),
                a.getStatus(),
                a.getReason(),
                a.getWindowStartedAt(),
                a.getAcceptedRequests(),
                a.getLlmCalls(),
                a.getPromptTokens(),
                a.getCompletionTokens(),
                a.getEstimatedCost(),
                a.getModel(),
                a.getCreatedAt(),
                a.getDecidedAt(),
                a.getDecidedBy(),
                a.getDecisionNote(),
                a.getRestrictionId());
    }

    private FerchoRestrictionRecord toRestrictionRecord(FerchoRestrictionEntity r) {
        return new FerchoRestrictionRecord(
                r.getId(),
                r.getUserId(),
                r.getType(),
                r.getReason(),
                r.getCreatedAt(),
                r.getCreatedBy(),
                r.getExpiresAt(),
                r.getLiftedAt(),
                r.getLiftedBy(),
                r.getNote());
    }
}
