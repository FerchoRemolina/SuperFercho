package com.superfercho.shopping.application.dto.shoppinglist;

import java.util.UUID;

public record RenameShoppingListCommand(UUID shoppingListId, String name) {}
