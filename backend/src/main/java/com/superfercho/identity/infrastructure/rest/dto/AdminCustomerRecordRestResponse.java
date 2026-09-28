package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.AdminCustomerRecordResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminCustomerRecordRestResponse(
        UUID id,
        String documentType,
        String documentNumber,
        String billingFirstName,
        String billingLastName,
        Instant createdAt,
        Instant updatedAt,
        List<AdminCustomerAccountRestResponse> accounts) {

    public static AdminCustomerRecordRestResponse from(AdminCustomerRecordResult result) {
        return new AdminCustomerRecordRestResponse(
                result.id(),
                result.documentType(),
                result.documentNumber(),
                result.billingFirstName(),
                result.billingLastName(),
                result.createdAt(),
                result.updatedAt(),
                result.accounts().stream().map(AdminCustomerAccountRestResponse::from).toList());
    }
}
