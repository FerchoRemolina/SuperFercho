package com.superfercho.catalog.infrastructure.rest.dto;

import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

/**
 * Contrato público y administrativo de producto. productTypeName /
 * productVariantName son aditivos y orientados a tienda: permiten al
 * Customer conocer la taxonomía real (Categoría → Tipo → Variante →
 * Presentación) sin resolver UUIDs en el frontend.
 */
public record ProductRestResponse(
        UUID id,
        UUID categoryId,
        UUID productTypeId,
        String productTypeName,
        UUID productVariantId,
        String productVariantName,
        Presentation presentation,
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
                result.productTypeId(),
                result.productTypeName(),
                result.productVariantId(),
                result.productVariantName(),
                result.presentation(),
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
