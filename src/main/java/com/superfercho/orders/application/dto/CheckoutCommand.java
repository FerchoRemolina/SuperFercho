package com.superfercho.orders.application.dto;

import com.superfercho.orders.application.exception.InvalidCheckoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CheckoutCommand(
        UUID addressId, PaymentMethod paymentMethod, List<CheckoutItem> items, String idempotencyKey) {

    public CheckoutCommand {
        items = items == null ? List.of() : List.copyOf(items);
        Set<UUID> productIds = new HashSet<>();
        for (CheckoutItem item : items) {
            if (item == null) {
                throw new InvalidCheckoutException("checkout items cannot contain null");
            }
            if (!productIds.add(item.productId())) {
                throw new InvalidCheckoutException("duplicate productId: " + item.productId());
            }
        }
    }
}
