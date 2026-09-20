package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record RegisteredCustomerRestResponse(
        UUID id,
        String documentType,
        String documentNumber,
        String fullName,
        String email,
        String phone,
        Role role,
        UserStatus status,
        Instant createdAt) {

    public static RegisteredCustomerRestResponse from(RegisteredCustomer customer) {
        return new RegisteredCustomerRestResponse(
                customer.id(),
                customer.documentType(),
                customer.documentNumber(),
                customer.fullName(),
                customer.email(),
                customer.phone(),
                customer.role(),
                customer.status(),
                customer.createdAt());
    }
}
