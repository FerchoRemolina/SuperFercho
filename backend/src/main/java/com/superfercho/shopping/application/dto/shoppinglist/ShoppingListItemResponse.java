package com.superfercho.shopping.application.dto.shoppinglist;

import com.superfercho.shopping.domain.model.ShoppingListItem;
import java.time.Instant;
import java.util.UUID;

public record ShoppingListItemResponse(UUID id, UUID productId, int quantity, Instant createdAt) {

    public static ShoppingListItemResponse from(ShoppingListItem item) {
        return new ShoppingListItemResponse(item.id(), item.productId(), item.quantity(), item.createdAt());
    }
}
