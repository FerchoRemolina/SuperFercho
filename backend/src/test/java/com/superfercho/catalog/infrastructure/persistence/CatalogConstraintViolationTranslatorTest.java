package com.superfercho.catalog.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.catalog.application.exception.DuplicateBarcodeException;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class CatalogConstraintViolationTranslatorTest {

    @Test
    void shouldTranslateDuplicateBarcodeConstraint() {
        DataIntegrityViolationException violation = violation("uk_catalog_products_barcode");

        RuntimeException translated = CatalogConstraintViolationTranslator.translate(violation);

        assertTrue(translated instanceof DuplicateBarcodeException);
        assertSame(violation, translated.getCause());
    }

    @Test
    void shouldTranslateMissingCategoryForeignKey() {
        DataIntegrityViolationException violation = violation("fk_catalog_products_category");

        RuntimeException translated = CatalogConstraintViolationTranslator.translate(violation);

        assertTrue(translated instanceof InvalidCategoryReferenceException);
        assertSame(violation, translated.getCause());
    }

    @Test
    void shouldRethrowUnknownIntegrityViolation() {
        DataIntegrityViolationException violation = violation("ck_catalog_products_currency");

        RuntimeException translated = CatalogConstraintViolationTranslator.translate(violation);

        assertSame(violation, translated);
    }

    private static DataIntegrityViolationException violation(String constraintName) {
        return new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"" + constraintName + "\"");
    }
}
