package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record GetCategoryCommand(UUID categoryId, CatalogView view) {
}
