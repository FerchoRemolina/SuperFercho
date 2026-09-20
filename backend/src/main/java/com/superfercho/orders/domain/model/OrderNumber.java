package com.superfercho.orders.domain.model;

import com.superfercho.orders.domain.exception.InvalidOrderException;

public record OrderNumber(String value) {

    public OrderNumber {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderException("orderNumber cannot be null or blank");
        }
    }
}
