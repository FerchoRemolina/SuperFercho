package com.superfercho.identity.application.dto;

import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;
import java.util.UUID;

public record ListAdminCustomerCommercialHistoryCommand(
        UUID customerRecordId, Integer page, Integer size) {

    public ListAdminCustomerCommercialHistoryCommand {
        if (customerRecordId == null) {
            throw new InvalidAdminCustomerQueryException("customerRecordId is required");
        }
    }
}
