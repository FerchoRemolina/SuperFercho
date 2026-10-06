package com.superfercho.identity.application.dto;

import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;
import java.util.Locale;

/** Ordenamiento soportado por el listado de CustomerRecords (whitelist). */
public enum AdminCustomerRecordSortBy {
    CREATED_AT,
    NAME,
    DOCUMENT,
    ORDERS,
    TOTAL_SPENT,
    LAST_ORDER_AT;

    public static AdminCustomerRecordSortBy parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return CREATED_AT;
        }
        try {
            return AdminCustomerRecordSortBy.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidAdminCustomerQueryException(
                    "sortBy must be CREATED_AT, NAME, DOCUMENT, ORDERS, TOTAL_SPENT or LAST_ORDER_AT");
        }
    }
}
