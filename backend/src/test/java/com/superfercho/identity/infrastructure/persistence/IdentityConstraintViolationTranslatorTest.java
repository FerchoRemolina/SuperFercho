package com.superfercho.identity.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.DuplicateDefaultAddressException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class IdentityConstraintViolationTranslatorTest {

    @Test
    void shouldTranslateDuplicateEmailConstraint() {
        DataIntegrityViolationException violation = violation("uk_identity_users_email");

        RuntimeException translated = IdentityConstraintViolationTranslator.translate(violation);

        assertTrue(translated instanceof UserAlreadyExistsException);
        assertSame(violation, translated.getCause());
    }

    @Test
    void shouldTranslateDuplicateDocumentConstraint() {
        DataIntegrityViolationException violation = violation("uk_identity_users_document");

        RuntimeException translated = IdentityConstraintViolationTranslator.translate(violation);

        assertTrue(translated instanceof DocumentAlreadyExistsException);
        assertSame(violation, translated.getCause());
    }

    @Test
    void shouldTranslateDuplicateDefaultAddressConstraint() {
        DataIntegrityViolationException violation = violation("uk_identity_addresses_one_active_default");

        RuntimeException translated = IdentityConstraintViolationTranslator.translate(violation);

        assertTrue(translated instanceof DuplicateDefaultAddressException);
        assertSame(violation, translated.getCause());
    }

    @Test
    void shouldRethrowUnknownIntegrityViolation() {
        DataIntegrityViolationException violation = violation("fk_identity_addresses_user_id");

        RuntimeException translated = IdentityConstraintViolationTranslator.translate(violation);

        assertSame(violation, translated);
    }

    private static DataIntegrityViolationException violation(String constraintName) {
        return new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"" + constraintName + "\"");
    }
}
