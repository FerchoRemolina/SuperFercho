package com.superfercho.assistant.application.fercho;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Restricciones activas e históricas de Fercho (pausas automáticas y
 * suspensiones administrativas). La creación de pausas automáticas ocurre
 * dentro de {@link FerchoUsageGate#evaluateAndReserve}; este puerto cubre las
 * operaciones administrativas.
 */
public interface FerchoRestrictionPort {

    Optional<FerchoRestrictionRecord> findActive(UUID userId, Instant now);

    Optional<FerchoRestrictionRecord> findById(UUID restrictionId);

    List<FerchoRestrictionRecord> findActive(int limit);

    List<FerchoRestrictionRecord> findForUser(UUID userId, int limit);

    UUID createAdminSuspension(UUID userId, UUID adminId, Instant expiresAt, String note, Instant now);

    void lift(UUID restrictionId, UUID adminId, String note, Instant now);
}
