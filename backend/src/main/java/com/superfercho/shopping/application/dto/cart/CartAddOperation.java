package com.superfercho.shopping.application.dto.cart;

import java.time.Instant;
import java.util.UUID;

public record CartAddOperation(
        UUID operationId, UUID customerId, UUID productId, int quantity, Instant createdAt) {

    public boolean matches(UUID customerId, UUID productId, int quantity) {
        return this.customerId.equals(customerId)
                && this.productId.equals(productId)
                && this.quantity == quantity;
    }
}
