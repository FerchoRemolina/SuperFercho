package com.superfercho.catalog.application.dto;

import java.util.List;
import java.util.UUID;

public record StockDecrementResult(boolean succeeded, List<UnavailableProduct> unavailableProducts) {

    public StockDecrementResult {
        unavailableProducts = List.copyOf(unavailableProducts);
    }

    public static StockDecrementResult success() {
        return new StockDecrementResult(true, List.of());
    }

    public static StockDecrementResult unavailable(List<UnavailableProduct> unavailableProducts) {
        return new StockDecrementResult(false, unavailableProducts);
    }

    public List<UUID> unavailableProductIds() {
        return unavailableProducts.stream().map(UnavailableProduct::productId).toList();
    }
}
