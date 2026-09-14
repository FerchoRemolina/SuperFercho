package com.superfercho.identity.application.dto;

import java.util.UUID;

public record UpdateAddressCommand(
        UUID userId,
        UUID addressId,
        String label,
        String recipientName,
        String addressLine,
        String additionalInfo,
        String city,
        String department,
        String phone) {
}
