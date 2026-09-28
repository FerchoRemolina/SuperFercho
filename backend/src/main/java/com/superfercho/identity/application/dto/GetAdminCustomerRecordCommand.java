package com.superfercho.identity.application.dto;

import java.util.UUID;

public record GetAdminCustomerRecordCommand(UUID customerRecordId, AdminAccountStatusFilter accountStatus) {

    public static GetAdminCustomerRecordCommand of(UUID customerRecordId, String accountStatus) {
        if (customerRecordId == null) {
            throw new com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException(
                    "customerRecordId is required");
        }
        return new GetAdminCustomerRecordCommand(customerRecordId, AdminAccountStatusFilter.parse(accountStatus));
    }
}
