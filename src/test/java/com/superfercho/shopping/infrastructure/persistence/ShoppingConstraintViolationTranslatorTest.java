package com.superfercho.shopping.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.shopping.application.exception.DuplicateCustomerCartException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ShoppingConstraintViolationTranslatorTest {

    @Test
    void shouldTranslateDuplicateCustomerCartConstraint() {
        DataIntegrityViolationException violation = violation("uk_shopping_carts_customer_id");

        RuntimeException translated = ShoppingConstraintViolationTranslator.translate(violation);

        assertTrue(translated instanceof DuplicateCustomerCartException);
        assertSame(violation, translated.getCause());
    }

    @Test
    void shouldRethrowUnknownIntegrityViolation() {
        DataIntegrityViolationException violation = violation("ck_shopping_carts_status");

        RuntimeException translated = ShoppingConstraintViolationTranslator.translate(violation);

        assertSame(violation, translated);
    }

    private static DataIntegrityViolationException violation(String constraintName) {
        return new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"" + constraintName + "\"");
    }
}
