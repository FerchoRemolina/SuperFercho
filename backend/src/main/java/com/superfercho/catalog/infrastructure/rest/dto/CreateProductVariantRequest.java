package com.superfercho.catalog.infrastructure.rest.dto;

import java.util.UUID;

public record CreateProductVariantRequest(UUID productTypeId, String name, String description) {
}
