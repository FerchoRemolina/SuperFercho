package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record CreateProductVariantCommand(UUID productTypeId, String name, String description) {
}
