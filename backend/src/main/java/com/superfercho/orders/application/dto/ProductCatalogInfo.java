package com.superfercho.orders.application.dto;

import com.superfercho.platform.money.Money;
import java.util.UUID;

public record ProductCatalogInfo(
        UUID productId, String name, Money currentPrice, boolean available, boolean active) {
}
