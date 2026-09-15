package com.superfercho.shopping.infrastructure.rest.dto;

public record CreateShoppingListRequest(String name) {

    public CreateShoppingListRequest {
        requireName(name);
    }

    static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("name must not exceed 255 characters");
        }
    }
}
