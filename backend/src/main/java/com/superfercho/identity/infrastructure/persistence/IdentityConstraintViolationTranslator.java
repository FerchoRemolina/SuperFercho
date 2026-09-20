package com.superfercho.identity.infrastructure.persistence;

import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.DuplicateDefaultAddressException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;

final class IdentityConstraintViolationTranslator {

    private static final String EMAIL_CONSTRAINT = "uk_identity_users_email";
    private static final String DOCUMENT_CONSTRAINT = "uk_identity_users_document";
    private static final String DEFAULT_ADDRESS_CONSTRAINT = "uk_identity_addresses_one_active_default";

    private IdentityConstraintViolationTranslator() {
    }

    static RuntimeException translate(DataIntegrityViolationException exception) {
        String detail = constraintDetail(exception);
        if (detail.contains(EMAIL_CONSTRAINT)) {
            return withCause(new UserAlreadyExistsException(), exception);
        }
        if (detail.contains(DOCUMENT_CONSTRAINT)) {
            return withCause(new DocumentAlreadyExistsException(), exception);
        }
        if (detail.contains(DEFAULT_ADDRESS_CONSTRAINT)) {
            return withCause(new DuplicateDefaultAddressException(), exception);
        }
        return exception;
    }

    private static RuntimeException withCause(RuntimeException translated, Throwable cause) {
        translated.initCause(cause);
        return translated;
    }

    private static String constraintDetail(Throwable throwable) {
        StringBuilder detail = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && constraintViolation.getConstraintName() != null) {
                detail.append(constraintViolation.getConstraintName()).append(' ');
            }
            if (current.getMessage() != null) {
                detail.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return detail.toString().toLowerCase();
    }
}
