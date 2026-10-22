package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.CustomerProfileResult;
import java.time.Instant;
import java.util.UUID;

/** Perfil del CUSTOMER autenticado. Sin passwordHash, rol, estado ni customerRecordId. */
public record CustomerProfileRestResponse(
        UUID id,
        String documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        Instant createdAt,
        Instant updatedAt) {

    public static CustomerProfileRestResponse from(CustomerProfileResult profile) {
        return new CustomerProfileRestResponse(
                profile.id(),
                profile.documentType(),
                profile.documentNumber(),
                profile.firstName(),
                profile.lastName(),
                profile.email(),
                profile.phone(),
                profile.createdAt(),
                profile.updatedAt());
    }
}
