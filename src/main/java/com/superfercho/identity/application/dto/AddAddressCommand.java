package com.superfercho.identity.application.dto;

import java.util.UUID;

public record AddAddressCommand(
        UUID userId,
        String label,
        String recipientName,
        String addressLine,
        String additionalInfo,
        String city,
        String department,
        String phone,
        boolean isDefault) {
}
