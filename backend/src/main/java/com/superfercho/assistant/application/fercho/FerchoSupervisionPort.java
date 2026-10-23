package com.superfercho.assistant.application.fercho;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Supervisión administrativa de Fercho (solo ADMIN): alertas, restricciones,
 * métricas agregadas y auditoría de decisiones.
 */
public interface FerchoSupervisionPort {

    /** Alertas; statusOrNull null = todas. */
    List<FerchoAlertRecord> findAlerts(String statusOrNull, int limit);

    Optional<FerchoAlertRecord> findAlert(UUID alertId);

    List<FerchoRestrictionRecord> findActiveRestrictions(int limit);

    List<FerchoRestrictionRecord> findRestrictionsForUser(UUID userId, int limit);

    Optional<FerchoRestrictionRecord> findRestriction(UUID restrictionId);

    FerchoClientUsageSummary clientUsage(UUID userId, Instant windowStart, Instant windowEnd);

    FerchoUsageTotals totals(Instant windowStart, Instant windowEnd);

    int countActiveAutoPauses(Instant now);

    int countActiveSuspensions(Instant now);

    int countOpenAlerts();

    /**
     * Cierra la alerta con la decisión del administrador y crea la suspensión
     * administrativa cuando la decisión es SUSPEND.
     *
     * @return id de la suspensión creada, o null si la decisión no la crea.
     */
    UUID resolveAlert(
            UUID alertId,
            FerchoAlertDecision decision,
            UUID adminId,
            String note,
            Duration suspensionDuration,
            Instant now);

    /** Levanta anticipadamente una suspensión activa (solo ADMIN). */
    void liftSuspension(UUID restrictionId, UUID adminId, String note, Instant now);
}
