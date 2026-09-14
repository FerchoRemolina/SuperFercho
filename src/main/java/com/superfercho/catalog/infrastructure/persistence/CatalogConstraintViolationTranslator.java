package com.superfercho.catalog.infrastructure.persistence;

import com.superfercho.catalog.application.exception.DuplicateBarcodeException;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import org.springframework.dao.DataIntegrityViolationException;

final class CatalogConstraintViolationTranslator {

    private static final String BARCODE_CONSTRAINT = "uk_catalog_products_barcode";
    private static final String CATEGORY_FK_CONSTRAINT = "fk_catalog_products_category";

    private CatalogConstraintViolationTranslator() {
    }

    static RuntimeException translate(DataIntegrityViolationException exception) {
        String detail = constraintDetail(exception);
        if (detail.contains(BARCODE_CONSTRAINT)) {
            return withCause(new DuplicateBarcodeException(), exception);
        }
        if (detail.contains(CATEGORY_FK_CONSTRAINT)) {
            return withCause(new InvalidCategoryReferenceException(), exception);
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
