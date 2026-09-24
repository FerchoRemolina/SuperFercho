package com.superfercho.catalog.domain.model;

import com.superfercho.catalog.domain.exception.InvalidProductVariantException;
import java.time.Instant;
import java.util.UUID;

public final class ProductVariant {

    private final UUID id;
    private final UUID productTypeId;
    private final String name;
    private final String description;
    private final ProductVariantStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ProductVariant(
            UUID id,
            UUID productTypeId,
            String name,
            String description,
            ProductVariantStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.productTypeId = productTypeId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductVariant create(
            UUID id,
            UUID productTypeId,
            String name,
            String description,
            ProductVariantStatus status,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireNonNull(productTypeId, "productTypeId");
        requireText(name, "name");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidProductVariantException("createdAt must not be after updatedAt");
        }

        return new ProductVariant(id, productTypeId, name, description, status, createdAt, updatedAt);
    }

    public ProductVariant updateInformation(String name, String description, Instant updatedAt) {
        return create(id, productTypeId, name, description, status, createdAt, updatedAt);
    }

    public ProductVariant activate(Instant updatedAt) {
        return create(id, productTypeId, name, description, ProductVariantStatus.ACTIVE, createdAt, updatedAt);
    }

    public ProductVariant deactivate(Instant updatedAt) {
        return create(
                id, productTypeId, name, description, ProductVariantStatus.INACTIVE, createdAt, updatedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID productTypeId() {
        return productTypeId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public ProductVariantStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidProductVariantException(field + " cannot be null");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidProductVariantException(field + " cannot be null or blank");
        }
    }
}
