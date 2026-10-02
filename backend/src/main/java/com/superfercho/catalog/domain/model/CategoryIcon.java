package com.superfercho.catalog.domain.model;

import com.superfercho.catalog.domain.exception.InvalidCategoryException;
import java.util.Locale;

/** Identificador estable del icono de una categoría (conjunto cerrado). */
public enum CategoryIcon {
    CLEANING,
    DRINKS,
    PERSONAL_CARE,
    GROCERY,
    FRUITS,
    BAKERY,
    MEAT,
    DAIRY,
    PETS,
    BABY,
    ELECTRONICS,
    HOME,
    OTHER;

    public static CategoryIcon parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidCategoryException("icon cannot be null or blank");
        }
        try {
            return CategoryIcon.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidCategoryException("Unsupported category icon: " + raw);
        }
    }
}
