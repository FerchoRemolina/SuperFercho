package com.superfercho.orders.application.exception;

import com.superfercho.platform.money.Money;
import java.util.UUID;

public class ProductPriceChangedException extends RuntimeException {

    private final UUID productId;
    private final Money expectedPrice;
    private final Money currentPrice;

    public ProductPriceChangedException(UUID productId, Money expectedPrice, Money currentPrice) {
        super("Product price changed: " + productId);
        this.productId = productId;
        this.expectedPrice = expectedPrice;
        this.currentPrice = currentPrice;
    }

    public UUID productId() {
        return productId;
    }

    public Money expectedPrice() {
        return expectedPrice;
    }

    public Money currentPrice() {
        return currentPrice;
    }
}
