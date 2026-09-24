package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record UpdateProductVariantCommand(UUID productVariantId, String name, String description) {
}
