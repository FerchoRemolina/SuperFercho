package com.superfercho.catalog.infrastructure.rest.dto;

import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.domain.model.CategoryIcon;
import com.superfercho.catalog.domain.model.CategoryStatus;
import java.time.Instant;
import java.util.UUID;

public record CategoryRestResponse(
        UUID id,
        String name,
        String description,
        CategoryIcon icon,
        CategoryStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static CategoryRestResponse from(CategoryResult result) {
        return new CategoryRestResponse(
                result.id(),
                result.name(),
                result.description(),
                result.icon(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }
}
