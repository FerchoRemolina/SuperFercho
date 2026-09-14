package com.superfercho.catalog.domain.model;

import com.superfercho.catalog.domain.exception.InvalidCategoryException;
import java.time.Instant;
import java.util.UUID;

public final class Category {

    private final UUID id;
    private final String name;
    private final String description;
    private final CategoryStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Category(
            UUID id,
            String name,
            String description,
            CategoryStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Category create(
            UUID id,
            String name,
            String description,
            CategoryStatus status,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireText(name, "name");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidCategoryException("createdAt must not be after updatedAt");
        }

        return new Category(id, name, description, status, createdAt, updatedAt);
    }

    public Category updateInformation(String name, String description, Instant updatedAt) {
        return create(id, name, description, status, createdAt, updatedAt);
    }

    public Category activate(Instant updatedAt) {
        return create(id, name, description, CategoryStatus.ACTIVE, createdAt, updatedAt);
    }

    public Category deactivate(Instant updatedAt) {
        return create(id, name, description, CategoryStatus.INACTIVE, createdAt, updatedAt);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public CategoryStatus status() {
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
            throw new InvalidCategoryException(field + " cannot be null");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidCategoryException(field + " cannot be null or blank");
        }
    }
}
