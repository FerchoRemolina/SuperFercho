package com.superfercho.identity.application.dto;

import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import java.time.Instant;
import java.util.UUID;

public record AddressResult(
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

    public static AddressResult from(Address address) {
        return new AddressResult(
                address.id(),
                address.label(),
                address.recipientName(),
                address.addressLine(),
                address.additionalInfo(),
                address.city(),
                address.department(),
                address.phone(),
                address.isDefault(),
                address.status(),
                address.createdAt(),
                address.updatedAt());
    }
}
