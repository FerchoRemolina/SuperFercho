package com.superfercho.shopping.application.dto.shoppinglist;

import java.util.UUID;

public record ChangeShoppingListItemQuantityCommand(UUID shoppingListId, UUID productId, int quantity) {}
