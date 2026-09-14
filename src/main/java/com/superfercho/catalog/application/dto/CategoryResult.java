package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import java.time.Instant;
import java.util.UUID;

public record CategoryResult(
        UUID id,
        String name,
        String description,
        CategoryStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static CategoryResult from(Category category) {
        return new CategoryResult(
                category.id(),
                category.name(),
                category.description(),
                category.status(),
                category.createdAt(),
                category.updatedAt());
    }
}
