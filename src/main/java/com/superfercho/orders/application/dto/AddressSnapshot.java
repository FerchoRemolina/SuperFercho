package com.superfercho.orders.application.dto;

public record AddressSnapshot(
        String recipientName,
        String addressLine,
        String additionalInfo,
        String city,
        String department,
        String phone) {
}
