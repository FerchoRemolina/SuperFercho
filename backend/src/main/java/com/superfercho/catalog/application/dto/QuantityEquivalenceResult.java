package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Structured quantity-to-package lookup. Never a cart mutation or checkout charge.
 */
public record QuantityEquivalenceResult(
        QuantityEquivalenceStatus status,
        String summary,
        UUID productId,
        String productName,
        Presentation presentation,
        Money currentPackagePrice,
        PresentationUnit baseUnit,
        BigDecimal packagePhysicalQuantityBase,
        BigDecimal requestedQuantity,
        PresentationUnit requestedUnit,
        BigDecimal requestedPhysicalQuantityBase,
        boolean exactMatch,
        Integer exactPackageCount,
        BigDecimal exactPhysicalTotalBase,
        QuantityPackageOption lowerAlternative,
        QuantityPackageOption upperAlternative,
        Integer catalogStock,
        boolean sellable,
        String availability,
        String unavailabilityReason) {

    public static QuantityEquivalenceResult notFound(UUID productId) {
        return unavailable(
                productId,
                null,
                null,
                null,
                null,
                null,
                false,
                "PRODUCT_NOT_FOUND",
                "Product is not in the public catalog.");
    }

    public static QuantityEquivalenceResult notSellable(
            UUID productId,
            String productName,
            Presentation presentation,
            BigDecimal requestedQuantity,
            PresentationUnit requestedUnit) {
        return unavailable(
                productId,
                productName,
                presentation,
                requestedQuantity,
                requestedUnit,
                null,
                false,
                "NOT_SELLABLE",
                "Product is not sellable in the public catalog.");
    }

    public static QuantityEquivalenceResult priceUnavailable(
            UUID productId,
            String productName,
            Presentation presentation,
            BigDecimal requestedQuantity,
            PresentationUnit requestedUnit,
            Integer catalogStock) {
        return unavailable(
                productId,
                productName,
                presentation,
                requestedQuantity,
                requestedUnit,
                catalogStock,
                false,
                "PRICE_UNAVAILABLE",
                "Current package price is unavailable.");
    }

    private static QuantityEquivalenceResult unavailable(
            UUID productId,
            String productName,
            Presentation presentation,
            BigDecimal requestedQuantity,
            PresentationUnit requestedUnit,
            Integer catalogStock,
            boolean sellable,
            String unavailabilityReason,
            String summary) {
        return new QuantityEquivalenceResult(
                QuantityEquivalenceStatus.NOT_AVAILABLE,
                summary,
                productId,
                productName,
                presentation,
                null,
                null,
                null,
                requestedQuantity,
                requestedUnit,
                null,
                false,
                null,
                null,
                null,
                null,
                catalogStock,
                sellable,
                unavailabilityReason,
                unavailabilityReason);
    }
}
