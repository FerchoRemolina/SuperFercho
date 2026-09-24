package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record CreateProductTypeCommand(UUID categoryId, String name, String description) {
}
