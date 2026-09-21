package com.superfercho.shopping.application.exception;

public class DuplicateFavoriteException extends RuntimeException {

    public DuplicateFavoriteException() {
        super("A favorite already exists for this customer and product");
    }
}
