package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record UpdateCategoryCommand(UUID categoryId, String name, String description) {
}
