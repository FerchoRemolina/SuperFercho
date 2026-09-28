package com.superfercho.identity.application.dto;

import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminCustomerAccountResult(
        UUID id,
        String email,
        String phone,
        UserStatus status,
        Instant createdAt,
        Instant deletedAt,
        UUID customerRecordId) {

    public static AdminCustomerAccountResult from(User user) {
        return new AdminCustomerAccountResult(
                user.id(),
                user.email(),
                user.phone(),
                user.status(),
                user.createdAt(),
                user.deletedAt(),
                user.customerRecordId());
    }
}
