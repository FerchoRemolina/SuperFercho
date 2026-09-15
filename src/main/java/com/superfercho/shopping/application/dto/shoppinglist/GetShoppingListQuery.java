package com.superfercho.shopping.application.dto.shoppinglist;

import java.util.UUID;

public record GetShoppingListQuery(UUID customerId, UUID shoppingListId) {}
