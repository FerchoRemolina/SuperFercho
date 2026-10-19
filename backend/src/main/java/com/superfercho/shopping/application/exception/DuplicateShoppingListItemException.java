package com.superfercho.shopping.application.exception;

public class DuplicateShoppingListItemException extends RuntimeException {

    public DuplicateShoppingListItemException() {
        super("The product is already in this shopping list");
    }
}
