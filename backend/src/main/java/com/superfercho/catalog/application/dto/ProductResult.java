package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

/**
 * Read model de producto. Los nombres de taxonomía (productTypeName /
 * productVariantName) son aditivos y opcionales: los flujos públicos de
 * lectura los resuelven desde el dominio; los flujos que no los resuelven
 * los dejan en null sin romper el contrato.
 */
public record ProductResult(
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

    public static ProductResult from(Product product) {
        return from(product, null, null);
    }

    public static ProductResult from(Product product, String productTypeName, String productVariantName) {
        return new ProductResult(
                product.id(),
                product.categoryId(),
                product.productTypeId(),
                productTypeName,
                product.productVariantId(),
                productVariantName,
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

    /** Constructor de compatibilidad con la aridad previa (sin nombres de taxonomía). */
    public ProductResult(
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
        this(
                id,
                categoryId,
                productTypeId,
                null,
                productVariantId,
                null,
                presentation,
                barcode,
                name,
                brand,
                description,
                price,
                stock,
                imageUrl,
                status,
                createdAt,
                updatedAt);
    }
}
