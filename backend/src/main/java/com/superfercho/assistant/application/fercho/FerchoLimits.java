package com.superfercho.assistant.application.fercho;

import java.time.Duration;

/**
 * Límites de uso de Fercho. Vienen de configuración (producción) o de valores
 * reducidos explícitos en pruebas; nunca se codifican en los servicios.
 *
 * @param maxRequestsPerWindow solicitudes aceptadas por cliente dentro de la
 *                             ventana móvil.
 * @param window               tamaño de la ventana móvil (24 h en producción).
 * @param pauseDuration        duración de la pausa automática (30 min en
 *                             producción).
 * @param recidiveWindow       ventana en la que se busca una pausa previa para
 *                             clasificar la reincidencia.
 */
public record FerchoLimits(
        int maxRequestsPerWindow,
        Duration window,
        Duration pauseDuration,
        Duration recidiveWindow
) {

    public FerchoLimits {
        if (maxRequestsPerWindow <= 0) {
            throw new IllegalArgumentException("maxRequestsPerWindow must be positive");
        }
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (pauseDuration == null || pauseDuration.isNegative() || pauseDuration.isZero()) {
            throw new IllegalArgumentException("pauseDuration must be positive");
        }
        if (recidiveWindow == null || recidiveWindow.isNegative() || recidiveWindow.isZero()) {
            throw new IllegalArgumentException("recidiveWindow must be positive");
        }
    }
}
