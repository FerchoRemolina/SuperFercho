package com.superfercho.catalog.infrastructure.rest.dto;

import com.superfercho.catalog.domain.model.Presentation;
import java.util.UUID;

public record UpdateProductRequest(
        UUID productTypeId,
        UUID productVariantId,
        Presentation presentation,
        String barcode,
        String name,
        String brand,
        String description,
        String imageUrl) {
}
