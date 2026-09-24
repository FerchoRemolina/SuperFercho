package com.superfercho.catalog.infrastructure.persistence;

import com.superfercho.catalog.application.exception.DuplicateBarcodeException;
import com.superfercho.catalog.application.exception.DuplicateProductTypeException;
import com.superfercho.catalog.application.exception.DuplicateProductVariantException;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.exception.InvalidProductTypeReferenceException;
import com.superfercho.catalog.application.exception.InvalidProductVariantReferenceException;
import org.springframework.dao.DataIntegrityViolationException;

final class CatalogConstraintViolationTranslator {

    private static final String BARCODE_CONSTRAINT = "uk_catalog_products_barcode";
    private static final String PRODUCT_TYPE_NAME_CONSTRAINT = "uk_catalog_product_types_category_name";
    private static final String PRODUCT_VARIANT_NAME_CONSTRAINT = "uk_catalog_product_variants_type_name";
    private static final String CATEGORY_FK_CONSTRAINT = "fk_catalog_products_category";
    private static final String PRODUCT_TYPE_FK_CONSTRAINT = "fk_catalog_products_product_type";
    private static final String PRODUCT_VARIANT_TYPE_FK_CONSTRAINT = "fk_catalog_products_variant_type";
    private static final String PRODUCT_TYPES_CATEGORY_FK = "fk_catalog_product_types_category";
    private static final String PRODUCT_VARIANTS_TYPE_FK = "fk_catalog_product_variants_type";

    private CatalogConstraintViolationTranslator() {
    }

    static RuntimeException translate(DataIntegrityViolationException exception) {
        String detail = constraintDetail(exception);
        if (detail.contains(BARCODE_CONSTRAINT)) {
            return withCause(new DuplicateBarcodeException(), exception);
        }
        if (detail.contains(PRODUCT_TYPE_NAME_CONSTRAINT)) {
            return withCause(new DuplicateProductTypeException(), exception);
        }
        if (detail.contains(PRODUCT_VARIANT_NAME_CONSTRAINT)) {
            return withCause(new DuplicateProductVariantException(), exception);
        }
        if (detail.contains(CATEGORY_FK_CONSTRAINT) || detail.contains(PRODUCT_TYPES_CATEGORY_FK)) {
            return withCause(new InvalidCategoryReferenceException(), exception);
        }
        if (detail.contains(PRODUCT_TYPE_FK_CONSTRAINT)) {
            return withCause(new InvalidProductTypeReferenceException(), exception);
        }
        if (detail.contains(PRODUCT_VARIANT_TYPE_FK_CONSTRAINT)
                || detail.contains(PRODUCT_VARIANTS_TYPE_FK)) {
            return withCause(new InvalidProductVariantReferenceException(), exception);
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
