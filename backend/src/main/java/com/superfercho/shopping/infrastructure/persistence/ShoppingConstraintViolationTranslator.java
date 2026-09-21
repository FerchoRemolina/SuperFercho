package com.superfercho.shopping.infrastructure.persistence;

import com.superfercho.shopping.application.exception.DuplicateCustomerCartException;
import com.superfercho.shopping.application.exception.DuplicateFavoriteException;
import org.springframework.dao.DataIntegrityViolationException;

final class ShoppingConstraintViolationTranslator {

    private static final String CUSTOMER_CART_CONSTRAINT = "uk_shopping_carts_customer_id";
    private static final String CUSTOMER_PRODUCT_FAVORITE_CONSTRAINT = "uk_shopping_favorites_customer_product";

    private ShoppingConstraintViolationTranslator() {
    }

    static RuntimeException translate(DataIntegrityViolationException exception) {
        String detail = constraintDetail(exception);
        if (detail.contains(CUSTOMER_CART_CONSTRAINT)) {
            return withCause(new DuplicateCustomerCartException(), exception);
        }
        if (detail.contains(CUSTOMER_PRODUCT_FAVORITE_CONSTRAINT)) {
            return withCause(new DuplicateFavoriteException(), exception);
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
