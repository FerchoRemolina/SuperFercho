package com.superfercho.identity.application.dto;

import com.superfercho.identity.domain.model.User;
import java.time.Instant;
import java.util.UUID;

/** Perfil del CUSTOMER autenticado. No expone passwordHash, rol, estado ni ids internos de negocio. */
public record CustomerProfileResult(
        UUID id,
        String documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        Instant createdAt,
        Instant updatedAt) {

    public static CustomerProfileResult from(User user) {
        return new CustomerProfileResult(
                user.id(),
                user.documentType(),
                user.documentNumber(),
                user.firstName(),
                user.lastName(),
                user.email(),
                user.phone(),
                user.createdAt(),
                user.updatedAt());
    }
}
