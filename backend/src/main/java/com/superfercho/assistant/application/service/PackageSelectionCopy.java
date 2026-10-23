package com.superfercho.assistant.application.service;

import com.superfercho.catalog.domain.model.PresentationUnit;
import java.math.BigDecimal;

public final class PackageSelectionCopy {

    private PackageSelectionCopy() {}

    public static String added(
            int packageCount,
            String productName,
            BigDecimal presentationQuantity,
            PresentationUnit presentationUnit) {
        String unit = presentationUnit == null ? "" : " " + presentationUnit.name();
        String presentation = presentationQuantity == null ? "" : presentationQuantity.stripTrailingZeros().toPlainString() + unit;
        return "Agregué " + packageCount + " paquete(s) de " + productName + " (" + presentation.trim() + ") al carrito.";
    }

    public static String alreadyInCart(int packageCount) {
        return "Esos " + packageCount + " paquete(s) ya estaban en el carrito.";
    }
}
