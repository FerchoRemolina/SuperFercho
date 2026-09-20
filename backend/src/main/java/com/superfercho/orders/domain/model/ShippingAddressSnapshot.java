package com.superfercho.orders.domain.model;

import com.superfercho.orders.domain.exception.InvalidOrderException;

public record ShippingAddressSnapshot(
        String recipientName,
        String addressLine,
        String additionalInfo,
        String city,
        String department,
        String phone) {

    public ShippingAddressSnapshot {
        requireText(recipientName, "recipientName");
        requireText(addressLine, "addressLine");
        requireText(city, "city");
        requireText(department, "department");
        requireText(phone, "phone");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderException(field + " cannot be null or blank");
        }
    }
}
