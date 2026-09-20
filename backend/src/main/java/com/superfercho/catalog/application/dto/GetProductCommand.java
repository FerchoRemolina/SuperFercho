package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record GetProductCommand(UUID productId, CatalogView view) {
}
