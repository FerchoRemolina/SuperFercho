package com.superfercho.assistant.application.packagesel;

import com.superfercho.catalog.domain.model.PresentationUnit;
import java.math.BigDecimal;
import java.util.Objects;

public record PackageSelectionOption(
        int packageCount, BigDecimal physicalTotalBase, PresentationUnit baseUnit) {

    public PackageSelectionOption {
        if (packageCount < 1) {
            throw new IllegalArgumentException("packageCount must be greater than 0");
        }
        Objects.requireNonNull(physicalTotalBase, "physicalTotalBase");
        Objects.requireNonNull(baseUnit, "baseUnit");
    }
}
