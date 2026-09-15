package com.superfercho.shopping.infrastructure.persistence;

import com.superfercho.shopping.application.exception.DuplicateCustomerCartException;
import org.springframework.dao.DataIntegrityViolationException;

final class ShoppingConstraintViolationTranslator {

    private static final String CUSTOMER_CART_CONSTRAINT = "uk_shopping_carts_customer_id";

    private ShoppingConstraintViolationTranslator() {
    }

    static RuntimeException translate(DataIntegrityViolationException exception) {
        String detail = constraintDetail(exception);
        if (detail.contains(CUSTOMER_CART_CONSTRAINT)) {
            return withCause(new DuplicateCustomerCartException(), exception);
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
