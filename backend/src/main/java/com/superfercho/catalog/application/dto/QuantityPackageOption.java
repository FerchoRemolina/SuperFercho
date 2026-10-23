package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.PresentationUnit;
import java.math.BigDecimal;

/**
 * Whole-package alternative or exact option. Not a cart mutation.
 *
 * @param differenceFromRequestedBase requested − packages, in the base unit; positive means shortfall
 */
public record QuantityPackageOption(
        int packageCount,
        BigDecimal physicalTotalBase,
        PresentationUnit baseUnit,
        BigDecimal differenceFromRequestedBase,
        String availability,
        String unavailabilityReason) {}
