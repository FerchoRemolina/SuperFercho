package com.superfercho.orders.application.exception;

import com.superfercho.catalog.application.dto.UnavailableProduct;
import java.util.List;

public class StockUnavailableException extends RuntimeException {

    private final List<UnavailableProduct> unavailableProducts;

    public StockUnavailableException(List<UnavailableProduct> unavailableProducts) {
        super("Stock is insufficient for one or more products");
        this.unavailableProducts = List.copyOf(unavailableProducts);
    }

    public List<UnavailableProduct> unavailableProducts() {
        return unavailableProducts;
    }
}
