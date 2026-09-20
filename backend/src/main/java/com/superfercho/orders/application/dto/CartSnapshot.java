package com.superfercho.orders.application.dto;

import java.util.List;
import java.util.UUID;

public record CartSnapshot(UUID cartId, List<CartItemSnapshot> items) {

    public CartSnapshot {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
