package com.superfercho.orders.application.dto;

public record ShippingAddressResult(
        String recipientName,
        String addressLine,
        String additionalInfo,
        String city,
        String department,
        String phone) {
}
