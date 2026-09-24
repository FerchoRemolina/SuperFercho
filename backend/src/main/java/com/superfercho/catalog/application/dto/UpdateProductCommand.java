package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.Presentation;
import java.util.UUID;

public record UpdateProductCommand(
        UUID productId,
        UUID productTypeId,
        UUID productVariantId,
        Presentation presentation,
        String barcode,
        String name,
        String brand,
        String description,
        String imageUrl) {
}
