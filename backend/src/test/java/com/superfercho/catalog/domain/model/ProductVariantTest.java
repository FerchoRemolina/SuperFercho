package com.superfercho.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.catalog.domain.exception.InvalidProductVariantException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductVariantTest {

    private static final UUID ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TYPE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T00:15:00Z");

    @Test
    void shouldCreateValidProductVariant() {
        ProductVariant variant = ProductVariant.create(
                ID, TYPE_ID, "Roja", null, ProductVariantStatus.ACTIVE, NOW, NOW);

        assertEquals(ID, variant.id());
        assertEquals(TYPE_ID, variant.productTypeId());
        assertEquals("Roja", variant.name());
        assertEquals(ProductVariantStatus.ACTIVE, variant.status());
    }

    @Test
    void shouldActivateAndDeactivate() {
        ProductVariant inactive = ProductVariant.create(
                ID, TYPE_ID, "Roja", null, ProductVariantStatus.INACTIVE, NOW, NOW);

        assertEquals(ProductVariantStatus.ACTIVE, inactive.activate(LATER).status());
        assertEquals(
                ProductVariantStatus.INACTIVE, inactive.activate(LATER).deactivate(LATER).status());
    }

    @Test
    void shouldRejectNullProductTypeId() {
        assertThrows(
                InvalidProductVariantException.class,
                () -> ProductVariant.create(ID, null, "Roja", null, ProductVariantStatus.ACTIVE, NOW, NOW));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void shouldRejectBlankName(String blank) {
        assertThrows(
                InvalidProductVariantException.class,
                () -> ProductVariant.create(ID, TYPE_ID, blank, null, ProductVariantStatus.ACTIVE, NOW, NOW));
    }

    @Test
    void shouldAcceptNameAtMaxLength() {
        String name = "n".repeat(ProductVariant.MAX_NAME_LENGTH);

        assertEquals(
                name,
                ProductVariant.create(ID, TYPE_ID, name, null, ProductVariantStatus.ACTIVE, NOW, NOW)
                        .name());
    }

    @Test
    void shouldRejectNameExceedingMaxLength() {
        InvalidProductVariantException error = assertThrows(
                InvalidProductVariantException.class,
                () -> ProductVariant.create(
                        ID,
                        TYPE_ID,
                        "n".repeat(ProductVariant.MAX_NAME_LENGTH + 1),
                        null,
                        ProductVariantStatus.ACTIVE,
                        NOW,
                        NOW));

        assertEquals("name cannot exceed 30 characters", error.getMessage());
    }
}
