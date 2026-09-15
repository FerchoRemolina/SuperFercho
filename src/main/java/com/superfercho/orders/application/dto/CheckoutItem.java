package com.superfercho.orders.application.dto;

import com.superfercho.orders.application.exception.InvalidCheckoutException;
import com.superfercho.platform.money.Money;
import java.util.UUID;

public record CheckoutItem(UUID productId, int quantity, Money expectedUnitPrice) {

    public CheckoutItem {
        if (productId == null) {
            throw new InvalidCheckoutException("productId cannot be null");
        }
        if (quantity <= 0) {
            throw new InvalidCheckoutException("quantity must be greater than 0");
        }
        if (expectedUnitPrice == null) {
            throw new InvalidCheckoutException("expectedUnitPrice cannot be null");
        }
    }
}
