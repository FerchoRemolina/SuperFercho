package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record UpdateProductCommand(
        UUID productId,
        UUID categoryId,
        String barcode,
        String name,
        String brand,
        String description,
        String imageUrl) {
}
