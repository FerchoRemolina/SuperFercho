package com.superfercho.identity.application.dto;

public record AddAddressCommand(
        String label,
        String recipientName,
        String addressLine,
        String additionalInfo,
        String city,
        String department,
        String phone,
        boolean isDefault) {
}
