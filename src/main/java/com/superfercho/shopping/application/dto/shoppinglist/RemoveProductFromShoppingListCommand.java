package com.superfercho.shopping.application.dto.shoppinglist;

import java.util.UUID;

public record RemoveProductFromShoppingListCommand(UUID shoppingListId, UUID productId) {}
