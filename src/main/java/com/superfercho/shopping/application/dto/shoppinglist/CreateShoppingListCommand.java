package com.superfercho.shopping.application.dto.shoppinglist;

import java.util.UUID;

public record CreateShoppingListCommand(UUID customerId, String name) {}
