package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.platform.money.Money;
import java.util.UUID;

public record CheckoutItemRequest(UUID productId, int quantity, Money expectedUnitPrice) {
}
