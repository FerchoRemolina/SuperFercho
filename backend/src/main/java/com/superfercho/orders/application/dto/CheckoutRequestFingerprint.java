package com.superfercho.orders.application.dto;

import java.util.Comparator;
import java.util.stream.Collectors;

public record CheckoutRequestFingerprint(String value) {

    public static CheckoutRequestFingerprint from(CheckoutCommand command) {
        String lines = command.items().stream()
                .sorted(Comparator.comparing(CheckoutItem::productId))
                .map(item -> item.productId()
                        + ":"
                        + item.quantity()
                        + ":"
                        + item.expectedUnitPrice().amount().toPlainString()
                        + ":"
                        + item.expectedUnitPrice().currency())
                .collect(Collectors.joining(","));
        return new CheckoutRequestFingerprint(
                command.addressId() + "|" + command.paymentMethod() + "|" + lines);
    }
}
