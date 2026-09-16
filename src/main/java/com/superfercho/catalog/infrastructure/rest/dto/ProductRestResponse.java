package com.superfercho.catalog.infrastructure.rest.dto;

import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

public record ProductRestResponse(
        UUID id,
        UUID categoryId,
        String barcode,
        String name,
        String brand,
        String description,
        Money price,
        int stock,
        String imageUrl,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductRestResponse from(ProductResult result) {
        return new ProductRestResponse(
                result.id(),
                result.categoryId(),
                result.barcode(),
                result.name(),
                result.brand(),
                result.description(),
                result.price(),
                result.stock(),
                result.imageUrl(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }
}
