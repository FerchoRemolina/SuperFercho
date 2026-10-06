package com.superfercho.identity.application.dto;

import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;
import java.util.Locale;

/** Filtro del listado administrativo de CustomerRecords ({@code status} query param). */
public enum AdminCustomerRecordStatusFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    CLOSED,
    NO_ACCOUNT;

    public static AdminCustomerRecordStatusFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return AdminCustomerRecordStatusFilter.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidAdminCustomerQueryException(
                    "status must be ALL, ACTIVE, INACTIVE, CLOSED, or NO_ACCOUNT");
        }
    }
}
