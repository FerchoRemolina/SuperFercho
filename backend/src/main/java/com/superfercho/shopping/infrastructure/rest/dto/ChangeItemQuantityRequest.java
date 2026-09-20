package com.superfercho.shopping.infrastructure.rest.dto;

public record ChangeItemQuantityRequest(int quantity) {

    public ChangeItemQuantityRequest {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }
}
