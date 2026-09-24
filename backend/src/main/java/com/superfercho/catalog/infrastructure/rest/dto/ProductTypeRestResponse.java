package com.superfercho.catalog.infrastructure.rest.dto;

import com.superfercho.catalog.application.dto.ProductTypeResult;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import java.time.Instant;
import java.util.UUID;

public record ProductTypeRestResponse(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        ProductTypeStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductTypeRestResponse from(ProductTypeResult result) {
        return new ProductTypeRestResponse(
                result.id(),
                result.categoryId(),
                result.name(),
                result.description(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }
}
