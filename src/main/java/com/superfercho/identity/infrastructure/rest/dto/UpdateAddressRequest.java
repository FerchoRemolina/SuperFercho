package com.superfercho.identity.infrastructure.rest.dto;

public record UpdateAddressRequest(
        String label,
        String recipientName,
        String addressLine,
        String additionalInfo,
        String city,
        String department,
        String phone) {
}
