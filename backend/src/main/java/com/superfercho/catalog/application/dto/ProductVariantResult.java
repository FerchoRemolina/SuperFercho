package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import java.time.Instant;
import java.util.UUID;

public record ProductVariantResult(
        UUID id,
        UUID productTypeId,
        String name,
        String description,
        ProductVariantStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductVariantResult from(ProductVariant productVariant) {
        return new ProductVariantResult(
                productVariant.id(),
                productVariant.productTypeId(),
                productVariant.name(),
                productVariant.description(),
                productVariant.status(),
                productVariant.createdAt(),
                productVariant.updatedAt());
    }
}
