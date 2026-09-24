package com.superfercho.catalog.domain.model;

import com.superfercho.catalog.domain.exception.InvalidProductTypeException;
import java.time.Instant;
import java.util.UUID;

public final class ProductType {

    private final UUID id;
    private final UUID categoryId;
    private final String name;
    private final String description;
    private final ProductTypeStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ProductType(
            UUID id,
            UUID categoryId,
            String name,
            String description,
            ProductTypeStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductType create(
            UUID id,
            UUID categoryId,
            String name,
            String description,
            ProductTypeStatus status,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireNonNull(categoryId, "categoryId");
        requireText(name, "name");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidProductTypeException("createdAt must not be after updatedAt");
        }

        return new ProductType(id, categoryId, name, description, status, createdAt, updatedAt);
    }

    public ProductType updateInformation(String name, String description, Instant updatedAt) {
        return create(id, categoryId, name, description, status, createdAt, updatedAt);
    }

    public ProductType activate(Instant updatedAt) {
        return create(id, categoryId, name, description, ProductTypeStatus.ACTIVE, createdAt, updatedAt);
    }

    public ProductType deactivate(Instant updatedAt) {
        return create(id, categoryId, name, description, ProductTypeStatus.INACTIVE, createdAt, updatedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID categoryId() {
        return categoryId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public ProductTypeStatus status() {
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
            throw new InvalidProductTypeException(field + " cannot be null");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidProductTypeException(field + " cannot be null or blank");
        }
    }
}
