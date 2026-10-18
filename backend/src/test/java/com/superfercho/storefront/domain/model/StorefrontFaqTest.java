package com.superfercho.storefront.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.storefront.domain.exception.InvalidStorefrontContentException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StorefrontFaqTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000fa01");

    @Test
    void createsFaqTrimmingTexts() {
        StorefrontFaq faq = StorefrontFaq.create(ID, "  ¿Pregunta?  ", "  Respuesta.  ", 10, true, NOW);

        assertEquals("¿Pregunta?", faq.question());
        assertEquals("Respuesta.", faq.answer());
        assertEquals(10, faq.displayOrder());
        assertEquals(true, faq.active());
        assertEquals(NOW, faq.createdAt());
        assertEquals(NOW, faq.updatedAt());
    }

    @Test
    void rejectsBlankQuestionAndAnswer() {
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> StorefrontFaq.create(ID, "   ", "Respuesta", 0, true, NOW));
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> StorefrontFaq.create(ID, "Pregunta", "   ", 0, true, NOW));
    }

    @Test
    void rejectsTooLongTexts() {
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> StorefrontFaq.create(ID, "x".repeat(301), "ok", 0, true, NOW));
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> StorefrontFaq.create(ID, "ok", "x".repeat(2001), 0, true, NOW));
    }

    @Test
    void rejectsNegativeDisplayOrder() {
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> StorefrontFaq.create(ID, "P", "R", -1, true, NOW));
    }

    @Test
    void withContentKeepsIdentityOrderAndDates() {
        StorefrontFaq faq = StorefrontFaq.create(ID, "P", "R", 10, true, NOW);
        StorefrontFaq updated = faq.withContent("P2", "R2");

        assertEquals(ID, updated.id());
        assertEquals("P2", updated.question());
        assertEquals("R2", updated.answer());
        assertEquals(10, updated.displayOrder());
        assertEquals(NOW, updated.createdAt());
    }

    @Test
    void withDisplayOrderAndActiveKeepTexts() {
        StorefrontFaq faq = StorefrontFaq.create(ID, "P", "R", 10, true, NOW);

        assertEquals(30, faq.withDisplayOrder(30).displayOrder());
        assertEquals(false, faq.withActive(false).active());
    }
}
