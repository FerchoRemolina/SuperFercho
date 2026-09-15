package com.superfercho.shopping.application.dto.shoppinglist;

import java.util.UUID;

public record AddProductToShoppingListCommand(
        UUID customerId, UUID shoppingListId, UUID productId, int quantity) {}
