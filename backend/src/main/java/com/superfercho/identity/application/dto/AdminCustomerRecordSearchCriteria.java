package com.superfercho.identity.application.dto;

import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;

/** Criterios del listado administrativo de CustomerRecords. */
public record AdminCustomerRecordSearchCriteria(
        String search,
        AdminCustomerRecordStatusFilter status,
        AdminCustomerRecordSortBy sortBy,
        AdminCustomerRecordSortDir sortDir,
        Boolean hasPurchases,
        int page,
        int size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public AdminCustomerRecordSearchCriteria {
        if (page < 0) {
            throw new InvalidAdminCustomerQueryException("page must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidAdminCustomerQueryException(
                    "size must be between 1 and " + MAX_SIZE);
        }
        if (status == null) {
            status = AdminCustomerRecordStatusFilter.ALL;
        }
        if (sortBy == null) {
            sortBy = AdminCustomerRecordSortBy.CREATED_AT;
        }
        if (sortDir == null) {
            sortDir = AdminCustomerRecordSortDir.DESC;
        }
    }

    public static AdminCustomerRecordSearchCriteria of(
            String search,
            String rawStatus,
            String rawSortBy,
            String rawSortDir,
            String rawHasPurchases,
            Integer page,
            Integer size) {
        return new AdminCustomerRecordSearchCriteria(
                search == null ? "" : search.trim(),
                AdminCustomerRecordStatusFilter.parse(rawStatus),
                AdminCustomerRecordSortBy.parse(rawSortBy),
                AdminCustomerRecordSortDir.parse(rawSortDir),
                parseHasPurchases(rawHasPurchases),
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size);
    }

    private static Boolean parseHasPurchases(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase();
        if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized)) {
            return Boolean.FALSE;
        }
        throw new InvalidAdminCustomerQueryException(
                "hasPurchases must be true, false, or omitted");
    }
}
