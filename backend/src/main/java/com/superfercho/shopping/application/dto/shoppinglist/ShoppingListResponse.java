package com.superfercho.shopping.application.dto.shoppinglist;

import com.superfercho.shopping.domain.model.ShoppingList;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShoppingListResponse(
        UUID id,
        UUID customerId,
        String name,
        List<ShoppingListItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    public static ShoppingListResponse from(ShoppingList shoppingList) {
        return new ShoppingListResponse(
                shoppingList.id(),
                shoppingList.customerId(),
                shoppingList.name(),
                shoppingList.items().stream().map(ShoppingListItemResponse::from).toList(),
                shoppingList.createdAt(),
                shoppingList.updatedAt());
    }
}
