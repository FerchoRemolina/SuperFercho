package com.superfercho.shopping.application.dto.cart;

public record IdempotentCartAddResult(CartResponse cart, boolean replayed) {

    public static IdempotentCartAddResult applied(CartResponse cart) {
        return new IdempotentCartAddResult(cart, false);
    }

    public static IdempotentCartAddResult replayed(CartResponse cart) {
        return new IdempotentCartAddResult(cart, true);
    }
}
