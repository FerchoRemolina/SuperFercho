package com.superfercho.assistant.infrastructure.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Límites de uso de Fercho. Valores de producción; las pruebas usan valores
 * reducidos explícitos y nunca estas cifras.
 */
@ConfigurationProperties(prefix = "superfercho.assistant.fercho.limits")
public record FerchoLimitsProperties(
        int maxRequestsPerWindow,
        Duration window,
        Duration pause,
        Duration recidiveWindow
) {

    public FerchoLimitsProperties {
        if (maxRequestsPerWindow <= 0) {
            throw new IllegalArgumentException(
                    "superfercho.assistant.fercho.limits.max-requests-per-window must be positive");
        }
        window = window == null ? Duration.ofHours(24) : window;
        pause = pause == null || pause.isZero() ? Duration.ofMinutes(30) : pause;
        recidiveWindow = recidiveWindow == null ? window : recidiveWindow;
    }
}
