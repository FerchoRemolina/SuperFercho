package com.superfercho.catalog.infrastructure.rest.dto;

import java.util.UUID;

public record CreateProductTypeRequest(UUID categoryId, String name, String description) {
}
