package com.superfercho.catalog.domain.model;

import com.superfercho.catalog.domain.exception.InvalidPresentationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Structured commercial presentation (quantity + unit), independent of stock units.
 */
public record Presentation(BigDecimal quantity, PresentationUnit unit) {

    public Presentation {
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(unit, "unit");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPresentationException("quantity must be greater than zero");
        }
        try {
            quantity = quantity.setScale(3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new InvalidPresentationException("quantity must have at most three decimal places");
        }
    }

    public static Presentation of(BigDecimal quantity, PresentationUnit unit) {
        return new Presentation(quantity, unit);
    }

    public static Presentation of(int quantity, PresentationUnit unit) {
        return new Presentation(BigDecimal.valueOf(quantity), unit);
    }
}
