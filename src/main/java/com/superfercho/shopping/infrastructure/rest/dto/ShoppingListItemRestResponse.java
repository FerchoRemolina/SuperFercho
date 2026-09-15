package com.superfercho.shopping.infrastructure.rest.dto;

import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListItemResponse;
import java.time.Instant;
import java.util.UUID;

public record ShoppingListItemRestResponse(UUID id, UUID productId, int quantity, Instant createdAt) {

    public static ShoppingListItemRestResponse from(ShoppingListItemResponse item) {
        return new ShoppingListItemRestResponse(item.id(), item.productId(), item.quantity(), item.createdAt());
    }
}
