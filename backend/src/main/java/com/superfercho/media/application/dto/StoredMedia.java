package com.superfercho.media.application.dto;

import com.superfercho.media.application.MediaImageFormat;

/**
 * Resultado interno del almacenamiento. {@code storageKey} nunca sale del
 * módulo: el contrato público expone únicamente la URL pública construida a
 * partir de la key.
 */
public record StoredMedia(String storageKey, String contentType) {

    public StoredMedia {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey cannot be null or blank");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType cannot be null or blank");
        }
    }

    public static StoredMedia of(String storageKey, MediaImageFormat format) {
        return new StoredMedia(storageKey, format.contentType());
    }
}
