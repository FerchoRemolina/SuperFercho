package com.superfercho.identity.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.identity.domain.exception.InvalidAddressException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AddressTest {

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void shouldCreateValidAddress() {
        Address address = validAddress().additionalInfo("Apto 101").build();

        assertEquals(ID, address.id());
        assertEquals("Casa", address.label());
        assertEquals("Ada Lovelace", address.recipientName());
        assertEquals("Calle 1 # 2-3", address.addressLine());
        assertEquals("Apto 101", address.additionalInfo());
        assertEquals("Bogotá", address.city());
        assertEquals("Cundinamarca", address.department());
        assertEquals("3001234567", address.phone());
        assertTrue(address.isDefault());
        assertEquals(AddressStatus.ACTIVE, address.status());
        assertEquals(CREATED_AT, address.createdAt());
        assertEquals(UPDATED_AT, address.updatedAt());
    }

    @Test
    void shouldRejectAddressWhenIdIsNull() {
        assertThrows(InvalidAddressException.class, () -> validAddress().id(null).build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectAddressWhenRequiredFieldIsBlank(String blank) {
        assertThrows(InvalidAddressException.class, () -> validAddress().label(blank).build());
        assertThrows(InvalidAddressException.class, () -> validAddress().recipientName(blank).build());
        assertThrows(InvalidAddressException.class, () -> validAddress().addressLine(blank).build());
        assertThrows(InvalidAddressException.class, () -> validAddress().city(blank).build());
        assertThrows(InvalidAddressException.class, () -> validAddress().department(blank).build());
        assertThrows(InvalidAddressException.class, () -> validAddress().phone(blank).build());
    }

    @Test
    void shouldAcceptAddressWhenAdditionalInfoIsNull() {
        Address address = validAddress().additionalInfo(null).build();

        assertNull(address.additionalInfo());
    }

    @Test
    void shouldAcceptAddressWhenAdditionalInfoIsBlank() {
        Address address = validAddress().additionalInfo("  ").build();

        assertEquals("  ", address.additionalInfo());
    }

    @Test
    void shouldRejectAddressWhenStatusIsNull() {
        assertThrows(InvalidAddressException.class, () -> validAddress().status(null).build());
    }

    @Test
    void shouldRejectAddressWhenCreatedAtIsAfterUpdatedAt() {
        assertThrows(
                InvalidAddressException.class,
                () -> validAddress()
                        .createdAt(Instant.parse("2026-01-02T00:00:00Z"))
                        .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                        .build());
    }

    @Test
    void shouldRejectInactiveAddressWhenMarkedDefault() {
        assertThrows(
                InvalidAddressException.class,
                () -> validAddress().isDefault(true).status(AddressStatus.INACTIVE).build());
    }

    @Test
    void shouldClearDefaultWhenDeactivatingAddress() {
        Address activeDefault = validAddress().isDefault(true).status(AddressStatus.ACTIVE).build();
        Instant deactivatedAt = Instant.parse("2026-01-01T00:15:00Z");

        Address deactivated = activeDefault.deactivate(deactivatedAt);

        assertFalse(deactivated.isDefault());
        assertEquals(AddressStatus.INACTIVE, deactivated.status());
        assertEquals(deactivatedAt, deactivated.updatedAt());
        assertEquals(activeDefault.id(), deactivated.id());
    }

    private static AddressBuilder validAddress() {
        return new AddressBuilder();
    }

    private static final class AddressBuilder {
        private UUID id = ID;
        private String label = "Casa";
        private String recipientName = "Ada Lovelace";
        private String addressLine = "Calle 1 # 2-3";
        private String additionalInfo = "Apto 101";
        private String city = "Bogotá";
        private String department = "Cundinamarca";
        private String phone = "3001234567";
        private boolean isDefault = true;
        private AddressStatus status = AddressStatus.ACTIVE;
        private Instant createdAt = CREATED_AT;
        private Instant updatedAt = UPDATED_AT;

        private AddressBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        private AddressBuilder label(String label) {
            this.label = label;
            return this;
        }

        private AddressBuilder recipientName(String recipientName) {
            this.recipientName = recipientName;
            return this;
        }

        private AddressBuilder addressLine(String addressLine) {
            this.addressLine = addressLine;
            return this;
        }

        private AddressBuilder additionalInfo(String additionalInfo) {
            this.additionalInfo = additionalInfo;
            return this;
        }

        private AddressBuilder city(String city) {
            this.city = city;
            return this;
        }

        private AddressBuilder department(String department) {
            this.department = department;
            return this;
        }

        private AddressBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        private AddressBuilder isDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        private AddressBuilder status(AddressStatus status) {
            this.status = status;
            return this;
        }

        private AddressBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        private AddressBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        private Address build() {
            return Address.create(
                    id,
                    label,
                    recipientName,
                    addressLine,
                    additionalInfo,
                    city,
                    department,
                    phone,
                    isDefault,
                    status,
                    createdAt,
                    updatedAt);
        }
    }
}
