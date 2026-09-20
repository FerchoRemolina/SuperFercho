package com.superfercho.shopping.infrastructure.rest.dto;

import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShoppingListRestResponse(
        UUID id,
        UUID customerId,
        String name,
        List<ShoppingListItemRestResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    public static ShoppingListRestResponse from(ShoppingListResponse shoppingList) {
        return new ShoppingListRestResponse(
                shoppingList.id(),
                shoppingList.customerId(),
                shoppingList.name(),
                shoppingList.items().stream().map(ShoppingListItemRestResponse::from).toList(),
                shoppingList.createdAt(),
                shoppingList.updatedAt());
    }
}
