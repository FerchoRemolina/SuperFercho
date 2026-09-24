package com.superfercho.catalog.infrastructure.rest.dto;

import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import java.time.Instant;
import java.util.UUID;

public record ProductVariantRestResponse(
        UUID id,
        UUID productTypeId,
        String name,
        String description,
        ProductVariantStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductVariantRestResponse from(ProductVariantResult result) {
        return new ProductVariantRestResponse(
                result.id(),
                result.productTypeId(),
                result.name(),
                result.description(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }
}
