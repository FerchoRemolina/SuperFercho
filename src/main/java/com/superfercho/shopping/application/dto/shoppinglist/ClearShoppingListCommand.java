package com.superfercho.shopping.application.dto.shoppinglist;

import java.util.UUID;

public record ClearShoppingListCommand(UUID customerId, UUID shoppingListId) {}
