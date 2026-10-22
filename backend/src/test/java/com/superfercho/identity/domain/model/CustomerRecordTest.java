package com.superfercho.identity.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.identity.domain.exception.InvalidCustomerRecordException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerRecordTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void shouldCreateValidCustomerRecord() {
        CustomerRecord record = CustomerRecord.create(
                ID, "CC", "12345678", "Ada", "Lovelace", NOW, NOW);

        assertEquals(ID, record.id());
        assertEquals("CC", record.documentType());
        assertEquals("12345678", record.documentNumber());
        assertEquals("Ada", record.billingFirstName());
        assertEquals("Lovelace", record.billingLastName());
        assertEquals(NOW, record.createdAt());
        assertEquals(NOW, record.updatedAt());
    }

    @Test
    void shouldRejectBlankDocumentType() {
        assertThrows(
                InvalidCustomerRecordException.class,
                () -> CustomerRecord.create(ID, "  ", "12345678", "Ada", "Lovelace", NOW, NOW));
    }

    @Test
    void shouldRejectBlankDocumentNumber() {
        assertThrows(
                InvalidCustomerRecordException.class,
                () -> CustomerRecord.create(ID, "CC", "", "Ada", "Lovelace", NOW, NOW));
    }

    @Test
    void shouldRejectBlankBillingFirstName() {
        assertThrows(
                InvalidCustomerRecordException.class,
                () -> CustomerRecord.create(ID, "CC", "12345678", " ", "Lovelace", NOW, NOW));
    }

    @Test
    void shouldRejectBlankBillingLastName() {
        assertThrows(
                InvalidCustomerRecordException.class,
                () -> CustomerRecord.create(ID, "CC", "12345678", "Ada", " ", NOW, NOW));
    }

    @Test
    void shouldRejectCreatedAtAfterUpdatedAt() {
        assertThrows(
                InvalidCustomerRecordException.class,
                () -> CustomerRecord.create(
                        ID,
                        "CC",
                        "12345678",
                        "Ada",
                        "Lovelace",
                        Instant.parse("2026-01-02T00:00:00Z"),
                        NOW));
    }

    @Test
    void shouldUpdateDocumentPreservingIdentity() {
        CustomerRecord record = CustomerRecord.create(ID, "CC", "12345678", "Ada", "Lovelace", NOW, NOW);
        Instant updatedAt = NOW.plusSeconds(3600);

        CustomerRecord updated = record.updateDocument("CE", "987654321", updatedAt);

        assertEquals(ID, updated.id());
        assertEquals("CE", updated.documentType());
        assertEquals("987654321", updated.documentNumber());
        assertEquals("Ada", updated.billingFirstName());
        assertEquals("Lovelace", updated.billingLastName());
        assertEquals(NOW, updated.createdAt());
        assertEquals(updatedAt, updated.updatedAt());
    }

    @Test
    void shouldRejectUpdateDocumentWithBlankNumber() {
        CustomerRecord record = CustomerRecord.create(ID, "CC", "12345678", "Ada", "Lovelace", NOW, NOW);

        assertThrows(
                InvalidCustomerRecordException.class,
                () -> record.updateDocument("CE", " ", NOW.plusSeconds(60)));
    }

    @Test
    void shouldRejectUpdateDocumentWithUpdatedAtBeforeCreatedAt() {
        CustomerRecord record = CustomerRecord.create(ID, "CC", "12345678", "Ada", "Lovelace", NOW, NOW);

        assertThrows(
                InvalidCustomerRecordException.class,
                () -> record.updateDocument("CE", "987654321", NOW.minusSeconds(60)));
    }
}
