package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.domain.model.AddressStatus;
import java.time.Instant;
import java.util.UUID;

public record AddressRestResponse(
        UUID id,
        String label,
        String recipientName,
        String addressLine,
        String additionalInfo,
        String city,
        String department,
        String phone,
        boolean isDefault,
        AddressStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static AddressRestResponse from(AddressResult result) {
        return new AddressRestResponse(
                result.id(),
                result.label(),
                result.recipientName(),
                result.addressLine(),
                result.additionalInfo(),
                result.city(),
                result.department(),
                result.phone(),
                result.isDefault(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }
}
