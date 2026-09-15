package com.superfercho.shopping.application.dto.shoppinglist;

import java.util.UUID;

public record RemoveProductFromShoppingListCommand(UUID customerId, UUID shoppingListId, UUID productId) {}
