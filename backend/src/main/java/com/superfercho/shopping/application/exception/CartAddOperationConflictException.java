package com.superfercho.shopping.application.exception;

import java.util.UUID;

public class CartAddOperationConflictException extends RuntimeException {

    public CartAddOperationConflictException(UUID operationId) {
        super("Cart add operation already applied with a different product or quantity: " + operationId);
    }
}
