package com.superfercho.identity.application.dto;

import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record RegisteredCustomer(
        UUID id,
        String documentType,
        String documentNumber,
        String fullName,
        String email,
        String phone,
        Role role,
        UserStatus status,
        Instant createdAt) {

    public static RegisteredCustomer from(User user) {
        return new RegisteredCustomer(
                user.id(),
                user.documentType(),
                user.documentNumber(),
                user.fullName(),
                user.email(),
                user.phone(),
                user.role(),
                user.status(),
                user.createdAt());
    }
}
