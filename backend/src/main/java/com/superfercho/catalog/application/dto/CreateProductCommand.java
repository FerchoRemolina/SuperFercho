package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.platform.money.Money;
import java.util.UUID;

public record CreateProductCommand(
        UUID productTypeId,
        UUID productVariantId,
        Presentation presentation,
        String barcode,
        String name,
        String brand,
        String description,
        Money price,
        int stock,
        String imageUrl) {
}
