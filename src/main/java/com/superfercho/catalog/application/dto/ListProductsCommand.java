package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.ProductStatus;
import java.util.UUID;

public record ListProductsCommand(UUID categoryId, ProductStatus status, CatalogView view) {
}
