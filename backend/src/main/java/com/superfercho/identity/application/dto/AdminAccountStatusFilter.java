package com.superfercho.identity.application.dto;

import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.util.Locale;

public enum AdminAccountStatusFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    DELETED;

    public static AdminAccountStatusFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return AdminAccountStatusFilter.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidAdminCustomerQueryException(
                    "accountStatus must be ALL, ACTIVE, INACTIVE, or DELETED");
        }
    }

    public boolean matches(User user) {
        return switch (this) {
            case ALL -> true;
            case ACTIVE -> user.deletedAt() == null && user.status() == UserStatus.ACTIVE;
            case INACTIVE -> user.deletedAt() == null && user.status() == UserStatus.INACTIVE;
            case DELETED -> user.deletedAt() != null;
        };
    }
}
