package com.superfercho.shopping.application.exception;

import java.util.UUID;

public class ShoppingListNotFoundException extends RuntimeException {

    public ShoppingListNotFoundException(UUID shoppingListId) {
        super("Shopping list not found: " + shoppingListId);
    }
}
