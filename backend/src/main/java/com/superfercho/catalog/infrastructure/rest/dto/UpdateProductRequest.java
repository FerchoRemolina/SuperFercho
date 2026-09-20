package com.superfercho.catalog.infrastructure.rest.dto;

import java.util.UUID;

public record UpdateProductRequest(
        UUID categoryId, String barcode, String name, String brand, String description, String imageUrl) {
}
