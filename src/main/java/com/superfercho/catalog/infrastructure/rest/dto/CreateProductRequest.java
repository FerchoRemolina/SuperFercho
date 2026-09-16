package com.superfercho.catalog.infrastructure.rest.dto;

import com.superfercho.platform.money.Money;
import java.util.UUID;

public record CreateProductRequest(
        UUID categoryId,
        String barcode,
        String name,
        String brand,
        String description,
        Money price,
        int stock,
        String imageUrl) {
}
