package com.superfercho.shopping.infrastructure.rest.dto;

import java.util.UUID;

public record AddItemRequest(UUID productId, int quantity) {

    public AddItemRequest {
        if (productId == null) {
            throw new IllegalArgumentException("productId cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }
}
