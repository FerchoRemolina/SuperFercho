package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

public record ProductResult(
        UUID id,
        UUID categoryId,
        UUID productTypeId,
        UUID productVariantId,
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

    public static ProductResult from(Product product) {
        return new ProductResult(
                product.id(),
                product.categoryId(),
                product.productTypeId(),
                product.productVariantId(),
                product.presentation(),
                product.barcode(),
                product.name(),
                product.brand(),
                product.description(),
                product.price(),
                product.stock(),
                product.imageUrl(),
                product.status(),
                product.createdAt(),
                product.updatedAt());
    }
}
