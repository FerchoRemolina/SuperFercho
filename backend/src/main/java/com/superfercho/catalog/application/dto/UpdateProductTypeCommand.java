package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record UpdateProductTypeCommand(UUID productTypeId, String name, String description) {
}
