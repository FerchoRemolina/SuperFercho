package com.superfercho.catalog.domain.model;

import com.superfercho.catalog.domain.exception.InvalidPresentationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Objects;

/**
 * Structured commercial presentation (quantity + unit), independent of stock units.
 */
public record Presentation(BigDecimal quantity, PresentationUnit unit) {

    private static final BigDecimal THOUSAND = new BigDecimal("1000");

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

    /**
     * Physical family for catalog ordering. MASS and VOLUME are never compared by quantity
     * against each other; DISCRETE units are not converted across each other.
     */
    public PresentationDimension dimension() {
        return switch (unit) {
            case G, KG -> PresentationDimension.MASS;
            case ML, L -> PresentationDimension.VOLUME;
            case UNIT, PACK, BOX, ROLL -> PresentationDimension.DISCRETE;
        };
    }

    /**
     * Quantity in a comparable base within {@link #dimension()}: grams, millilitres, or raw
     * discrete quantity. Does not alter the stored {@link #quantity()} / {@link #unit()}.
     */
    public BigDecimal normalizedComparableQuantity() {
        return switch (unit) {
            case G, ML, UNIT, PACK, BOX, ROLL -> quantity;
            case KG, L -> quantity.multiply(THOUSAND);
        };
    }

    /**
     * Deterministic catalog order: dimension (MASS &lt; VOLUME &lt; DISCRETE), then within DISCRETE
     * unit name A-Z, then normalized quantity descending.
     */
    public static Comparator<Presentation> catalogOrder() {
        return Comparator.comparing(Presentation::dimension)
                .thenComparing(
                        Presentation::unit,
                        (left, right) -> {
                            if (left == right) {
                                return 0;
                            }
                            boolean discrete = isDiscrete(left) && isDiscrete(right);
                            if (!discrete) {
                                return 0;
                            }
                            return left.name().compareTo(right.name());
                        })
                .thenComparing(Presentation::normalizedComparableQuantity, Comparator.reverseOrder());
    }

    private static boolean isDiscrete(PresentationUnit unit) {
        return unit == PresentationUnit.UNIT
                || unit == PresentationUnit.PACK
                || unit == PresentationUnit.BOX
                || unit == PresentationUnit.ROLL;
    }
}
