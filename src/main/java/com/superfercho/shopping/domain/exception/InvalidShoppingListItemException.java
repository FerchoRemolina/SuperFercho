package com.superfercho.shopping.domain.exception;

public class InvalidShoppingListItemException extends RuntimeException {

    public InvalidShoppingListItemException(String message) {
        super(message);
    }
}
