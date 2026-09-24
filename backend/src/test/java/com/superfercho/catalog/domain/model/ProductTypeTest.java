package com.superfercho.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.catalog.domain.exception.InvalidProductTypeException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductTypeTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T00:15:00Z");

    @Test
    void shouldCreateValidProductType() {
        ProductType type = ProductType.create(
                ID, CATEGORY_ID, "Manzana", "Fruta", ProductTypeStatus.ACTIVE, NOW, NOW);

        assertEquals(ID, type.id());
        assertEquals(CATEGORY_ID, type.categoryId());
        assertEquals("Manzana", type.name());
        assertEquals(ProductTypeStatus.ACTIVE, type.status());
    }

    @Test
    void shouldActivateAndDeactivate() {
        ProductType inactive = ProductType.create(
                ID, CATEGORY_ID, "Manzana", null, ProductTypeStatus.INACTIVE, NOW, NOW);

        assertEquals(ProductTypeStatus.ACTIVE, inactive.activate(LATER).status());
        assertEquals(ProductTypeStatus.INACTIVE, inactive.activate(LATER).deactivate(LATER).status());
    }

    @Test
    void shouldUpdateInformationPreservingIdentity() {
        ProductType original = ProductType.create(
                ID, CATEGORY_ID, "Manzana", "Old", ProductTypeStatus.ACTIVE, NOW, NOW);

        ProductType updated = original.updateInformation("Manzana roja", "New", LATER);

        assertEquals(ID, updated.id());
        assertEquals(CATEGORY_ID, updated.categoryId());
        assertEquals("Manzana roja", updated.name());
        assertEquals("New", updated.description());
        assertEquals(NOW, updated.createdAt());
        assertEquals(LATER, updated.updatedAt());
    }

    @Test
    void shouldRejectNullCategoryId() {
        assertThrows(
                InvalidProductTypeException.class,
                () -> ProductType.create(ID, null, "Manzana", null, ProductTypeStatus.ACTIVE, NOW, NOW));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void shouldRejectBlankName(String blank) {
        assertThrows(
                InvalidProductTypeException.class,
                () -> ProductType.create(ID, CATEGORY_ID, blank, null, ProductTypeStatus.ACTIVE, NOW, NOW));
    }
}
