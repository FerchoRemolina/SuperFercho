package com.superfercho.identity.application.dto;

import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;
import java.util.Locale;

public enum AdminCustomerRecordSortDir {
    ASC,
    DESC;

    public static AdminCustomerRecordSortDir parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DESC;
        }
        try {
            return AdminCustomerRecordSortDir.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidAdminCustomerQueryException(
                    "sortDir must be ASC or DESC");
        }
    }
}
