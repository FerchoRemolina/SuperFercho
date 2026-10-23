package com.superfercho.catalog.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Deterministic conversion of a requested mass/volume into whole SKU packages.
 * Does not round, does not convert MASS↔VOLUME, and does not convert DISCRETE units.
 */
public final class PackageQuantityEquivalence {

    private PackageQuantityEquivalence() {}

    public static Match match(Presentation packagePresentation, Presentation requested) {
        Objects.requireNonNull(packagePresentation, "packagePresentation");
        Objects.requireNonNull(requested, "requested");
        if (!convertibleTogether(packagePresentation, requested)) {
            return Match.incompatible();
        }
        BigDecimal packageBase = packagePresentation.normalizedComparableQuantity();
        BigDecimal requestedBase = requested.normalizedComparableQuantity();
        PresentationUnit baseUnit = baseUnitOf(packagePresentation.dimension());
        BigDecimal[] parts = requestedBase.divideAndRemainder(packageBase);
        BigDecimal quotient = parts[0];
        BigDecimal remainder = parts[1];
        Integer floor = toPackageCount(quotient);
        boolean exact = remainder.compareTo(BigDecimal.ZERO) == 0 && floor != null;
        Integer ceil = remainder.compareTo(BigDecimal.ZERO) == 0
                ? null
                : toPackageCount(quotient.add(BigDecimal.ONE));
        Option exactOption = exact ? option(floor, packageBase, requestedBase, baseUnit) : null;
        Option lower = !exact && floor != null ? option(floor, packageBase, requestedBase, baseUnit) : null;
        Option upper = !exact && ceil != null ? option(ceil, packageBase, requestedBase, baseUnit) : null;
        return new Match(true, exact, packageBase, requestedBase, baseUnit, exactOption, lower, upper);
    }

    private static boolean convertibleTogether(Presentation left, Presentation right) {
        PresentationDimension dimension = left.dimension();
        return dimension == right.dimension()
                && (dimension == PresentationDimension.MASS || dimension == PresentationDimension.VOLUME);
    }

    private static PresentationUnit baseUnitOf(PresentationDimension dimension) {
        return switch (dimension) {
            case MASS -> PresentationUnit.G;
            case VOLUME -> PresentationUnit.ML;
            case DISCRETE -> throw new IllegalStateException("discrete units are not convertible");
        };
    }

    private static Integer toPackageCount(BigDecimal quotient) {
        if (quotient.compareTo(BigDecimal.ONE) < 0) {
            return null;
        }
        try {
            return quotient.intValueExact();
        } catch (ArithmeticException exception) {
            return null;
        }
    }

    private static Option option(
            int count, BigDecimal packageBase, BigDecimal requestedBase, PresentationUnit baseUnit) {
        BigDecimal total = packageBase.multiply(BigDecimal.valueOf(count));
        return new Option(count, total, baseUnit, requestedBase.subtract(total));
    }

    public record Match(
            boolean compatible,
            boolean exactMatch,
            BigDecimal packagePhysicalQuantityBase,
            BigDecimal requestedPhysicalQuantityBase,
            PresentationUnit baseUnit,
            Option exact,
            Option lowerAlternative,
            Option upperAlternative) {

        public static Match incompatible() {
            return new Match(false, false, null, null, null, null, null, null);
        }
    }

    /**
     * @param differenceFromRequestedBase requested − packages; positive means shortfall
     */
    public record Option(
            int packageCount,
            BigDecimal physicalTotalBase,
            PresentationUnit baseUnit,
            BigDecimal differenceFromRequestedBase) {}
}
