package com.superfercho.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.catalog.domain.exception.InvalidCategoryException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CategoryTest {

    private static final UUID ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T00:15:00Z");

    @Test
    void shouldCreateValidCategory() {
        Category category = validCategory().description("Fresh produce").build();

        assertEquals(ID, category.id());
        assertEquals("Frutas", category.name());
        assertEquals("Fresh produce", category.description());
        assertEquals(CategoryStatus.ACTIVE, category.status());
        assertEquals(CREATED_AT, category.createdAt());
        assertEquals(UPDATED_AT, category.updatedAt());
    }

    @Test
    void shouldAcceptCategoryWhenDescriptionIsNull() {
        Category category = validCategory().description(null).build();

        assertNull(category.description());
    }

    @Test
    void shouldRejectCategoryWhenIdIsNull() {
        assertThrows(InvalidCategoryException.class, () -> validCategory().id(null).build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectCategoryWhenNameIsBlank(String blank) {
        assertThrows(InvalidCategoryException.class, () -> validCategory().name(blank).build());
    }

    @Test
    void shouldRejectCategoryWhenStatusIsNull() {
        assertThrows(InvalidCategoryException.class, () -> validCategory().status(null).build());
    }

    @Test
    void shouldRejectCategoryWhenCreatedAtIsNull() {
        assertThrows(InvalidCategoryException.class, () -> validCategory().createdAt(null).build());
    }

    @Test
    void shouldRejectCategoryWhenUpdatedAtIsNull() {
        assertThrows(InvalidCategoryException.class, () -> validCategory().updatedAt(null).build());
    }

    @Test
    void shouldActivateCategory() {
        Category inactive = validCategory().status(CategoryStatus.INACTIVE).build();

        Category activated = inactive.activate(LATER);

        assertEquals(CategoryStatus.ACTIVE, activated.status());
        assertEquals(inactive.id(), activated.id());
        assertEquals(inactive.name(), activated.name());
        assertEquals(inactive.createdAt(), activated.createdAt());
        assertEquals(LATER, activated.updatedAt());
    }

    @Test
    void shouldDeactivateCategory() {
        Category active = validCategory().build();

        Category deactivated = active.deactivate(LATER);

        assertEquals(CategoryStatus.INACTIVE, deactivated.status());
        assertEquals(active.id(), deactivated.id());
        assertEquals(active.name(), deactivated.name());
        assertEquals(active.createdAt(), deactivated.createdAt());
        assertEquals(LATER, deactivated.updatedAt());
    }

    @Test
    void shouldUpdateCategoryInformationAndPreserveIdentity() {
        Category original = validCategory().build();

        Category updated = original.updateInformation("Verduras", "Leafy greens", LATER);

        assertEquals(original.id(), updated.id());
        assertEquals(original.status(), updated.status());
        assertEquals(original.createdAt(), updated.createdAt());
        assertEquals("Verduras", updated.name());
        assertEquals("Leafy greens", updated.description());
        assertEquals(LATER, updated.updatedAt());
    }

    @Test
    void shouldRejectInformationUpdateWhenNameIsBlank() {
        Category original = validCategory().build();

        assertThrows(
                InvalidCategoryException.class, () -> original.updateInformation("  ", "Leafy greens", LATER));
    }

    private static CategoryBuilder validCategory() {
        return new CategoryBuilder();
    }

    private static final class CategoryBuilder {
        private UUID id = ID;
        private String name = "Frutas";
        private String description = "Fresh produce";
        private CategoryStatus status = CategoryStatus.ACTIVE;
        private Instant createdAt = CREATED_AT;
        private Instant updatedAt = UPDATED_AT;

        private CategoryBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        private CategoryBuilder name(String name) {
            this.name = name;
            return this;
        }

        private CategoryBuilder description(String description) {
            this.description = description;
            return this;
        }

        private CategoryBuilder status(CategoryStatus status) {
            this.status = status;
            return this;
        }

        private CategoryBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        private CategoryBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        private Category build() {
            return Category.create(id, name, description, status, createdAt, updatedAt);
        }
    }
}
