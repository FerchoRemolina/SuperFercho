package com.superfercho.shopping.infrastructure.rest.dto;

public record RenameShoppingListRequest(String name) {

    public RenameShoppingListRequest {
        CreateShoppingListRequest.requireName(name);
    }
}
