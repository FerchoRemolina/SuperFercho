package com.superfercho.media.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo media. {@code dir} es el directorio base del
 * almacenamiento filesystem (default ./data/media), configurable con
 * SUPERFERCHO_MEDIA_DIR.
 */
@ConfigurationProperties(prefix = "superfercho.media")
public record MediaProperties(String dir) {

    public MediaProperties {
        if (dir == null || dir.isBlank()) {
            dir = "./data/media";
        }
    }
}
