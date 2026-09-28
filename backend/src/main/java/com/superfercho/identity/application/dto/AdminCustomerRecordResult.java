package com.superfercho.identity.application.dto;

import com.superfercho.identity.domain.model.CustomerRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminCustomerRecordResult(
        UUID id,
        String documentType,
        String documentNumber,
        String billingFirstName,
        String billingLastName,
        Instant createdAt,
        Instant updatedAt,
        List<AdminCustomerAccountResult> accounts) {

    public static AdminCustomerRecordResult from(
            CustomerRecord record, List<AdminCustomerAccountResult> accounts) {
        return new AdminCustomerRecordResult(
                record.id(),
                record.documentType(),
                record.documentNumber(),
                record.billingFirstName(),
                record.billingLastName(),
                record.createdAt(),
                record.updatedAt(),
                List.copyOf(accounts));
    }
}
