package com.superfercho.identity.application.dto;

import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;
import java.util.UUID;

public record ActivateAdminCustomerAccountCommand(UUID customerRecordId, UUID userId) {

    public ActivateAdminCustomerAccountCommand {
        if (customerRecordId == null) {
            throw new InvalidAdminCustomerQueryException("customerRecordId is required");
        }
        if (userId == null) {
            throw new InvalidAdminCustomerQueryException("userId is required");
        }
    }
}
