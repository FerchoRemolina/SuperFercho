package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.AdminCustomerAccountResult;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminCustomerAccountRestResponse(
        UUID id,
        String email,
        String phone,
        UserStatus status,
        Instant createdAt,
        Instant deletedAt,
        UUID customerRecordId) {

    public static AdminCustomerAccountRestResponse from(AdminCustomerAccountResult account) {
        return new AdminCustomerAccountRestResponse(
                account.id(),
                account.email(),
                account.phone(),
                account.status(),
                account.createdAt(),
                account.deletedAt(),
                account.customerRecordId());
    }
}
