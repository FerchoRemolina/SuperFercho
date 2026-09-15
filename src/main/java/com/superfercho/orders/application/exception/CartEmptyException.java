package com.superfercho.orders.application.exception;

public class CartEmptyException extends RuntimeException {

    public CartEmptyException() {
        super("Active cart is empty");
    }
}
