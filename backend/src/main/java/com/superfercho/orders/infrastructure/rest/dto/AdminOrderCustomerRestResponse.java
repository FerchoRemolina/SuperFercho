package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.CustomerDirectoryEntry;
import java.util.UUID;

public record AdminOrderCustomerRestResponse(
        UUID userId,
        String fullName,
        String documentType,
        String documentNumber,
        String email,
        String phone) {

    public static AdminOrderCustomerRestResponse from(CustomerDirectoryEntry entry) {
        return new AdminOrderCustomerRestResponse(
                entry.userId(),
                entry.fullName(),
                entry.documentType(),
                entry.documentNumber(),
                entry.email(),
                entry.phone());
    }
}
