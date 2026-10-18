package com.superfercho.storefront.domain.model;

import com.superfercho.storefront.domain.exception.InvalidStorefrontContentException;

/** Modos de presentación del Hero. Solo SuperFercho define el diseño. */
public enum HeroPresentationMode {
    STANDARD,
    IMMERSIVE;

    public static HeroPresentationMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidStorefrontContentException("heroMode es obligatorio");
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidStorefrontContentException(
                    "heroMode debe ser STANDARD o IMMERSIVE: " + raw);
        }
    }
}
