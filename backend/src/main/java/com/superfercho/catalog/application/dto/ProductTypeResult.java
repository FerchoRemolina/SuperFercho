package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import java.time.Instant;
import java.util.UUID;

public record ProductTypeResult(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        ProductTypeStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductTypeResult from(ProductType productType) {
        return new ProductTypeResult(
                productType.id(),
                productType.categoryId(),
                productType.name(),
                productType.description(),
                productType.status(),
                productType.createdAt(),
                productType.updatedAt());
    }
}
