package com.superfercho.shopping.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.shopping.domain.exception.InvalidShoppingListException;
import com.superfercho.shopping.domain.exception.InvalidShoppingListItemException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ShoppingListTest {

    private static final UUID LIST_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID MILK_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BREAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Instant CREATED_AT = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-04-01T10:05:00Z");

    @Test
    void shouldCreateEmptyShoppingList() {
        ShoppingList list = validList().build();

        assertEquals(LIST_ID, list.id());
        assertEquals(CUSTOMER_ID, list.customerId());
        assertEquals("Mercado semanal", list.name());
        assertTrue(list.items().isEmpty());
        assertEquals(CREATED_AT, list.createdAt());
        assertEquals(CREATED_AT, list.updatedAt());
    }

    @Test
    void shouldCreateShoppingListWithItems() {
        ShoppingListItem milk = milk(2);
        ShoppingList list = validList().items(List.of(milk)).build();

        assertEquals(1, list.items().size());
        assertEquals(milk, list.items().get(0));
    }

    @Test
    void shouldRejectListWhenIdIsNull() {
        assertThrows(InvalidShoppingListException.class, () -> validList().id(null).build());
    }

    @Test
    void shouldRejectListWhenCustomerIdIsNull() {
        assertThrows(InvalidShoppingListException.class, () -> validList().customerId(null).build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectListWhenNameIsBlank(String blank) {
        assertThrows(InvalidShoppingListException.class, () -> validList().name(blank).build());
    }

    @Test
    void shouldRejectListWhenCreatedAtIsNull() {
        assertThrows(InvalidShoppingListException.class, () -> validList().createdAt(null).build());
    }

    @Test
    void shouldRejectListWhenUpdatedAtIsNull() {
        assertThrows(InvalidShoppingListException.class, () -> validList().updatedAt(null).build());
    }

    @Test
    void shouldRejectListWhenCreatedAtIsAfterUpdatedAt() {
        assertThrows(
                InvalidShoppingListException.class,
                () -> validList().updatedAt(CREATED_AT.minusSeconds(1)).build());
    }

    @Test
    void shouldRejectListWhenItemsIsNull() {
        assertThrows(InvalidShoppingListException.class, () -> validList().items(null).build());
    }

    @Test
    void shouldRejectListWhenItemsContainDuplicateProductId() {
        assertThrows(
                InvalidShoppingListException.class,
                () -> validList().items(List.of(milk(1), milk(3))).build());
    }

    @Test
    void shouldKeepItemSnapshotIndependentFromOriginalList() {
        ShoppingListItem item = milk(1);
        List<ShoppingListItem> items = new ArrayList<>();
        items.add(item);

        ShoppingList list = validList().items(items).build();
        items.clear();

        assertEquals(1, list.items().size());
        assertEquals(item, list.items().get(0));
    }

    @Test
    void shouldExposeItemsAsUnmodifiableCollection() {
        ShoppingList list = validList().items(List.of(milk(1))).build();

        assertThrows(UnsupportedOperationException.class, () -> list.items().add(bread(1)));
        assertThrows(UnsupportedOperationException.class, () -> list.items().clear());
    }

    @Test
    void shouldChangeNameAndPreserveIdentity() {
        ShoppingList original = validList().build();

        ShoppingList renamed = original.changeName("Fin de semana", LATER);

        assertEquals(LIST_ID, renamed.id());
        assertEquals(CUSTOMER_ID, renamed.customerId());
        assertEquals("Fin de semana", renamed.name());
        assertEquals(CREATED_AT, renamed.createdAt());
        assertEquals(LATER, renamed.updatedAt());
        assertEquals("Mercado semanal", original.name());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectNameChangeWhenNameIsBlank(String blank) {
        assertThrows(InvalidShoppingListException.class, () -> validList().build().changeName(blank, LATER));
    }

    @Test
    void shouldAddProductToEmptyList() {
        ShoppingList list = validList().build().addProduct(itemId(1), MILK_ID, 2, LATER);

        assertEquals(1, list.items().size());
        assertEquals(MILK_ID, list.items().get(0).productId());
        assertEquals(2, list.items().get(0).quantity());
        assertEquals(LATER, list.items().get(0).createdAt());
        assertEquals(LATER, list.updatedAt());
    }

    @Test
    void shouldIncrementQuantityWhenProductAlreadyExists() {
        ShoppingListItem existing = milk(2);
        ShoppingList list = validList().items(List.of(existing)).build().addProduct(itemId(99), MILK_ID, 3, LATER);

        assertEquals(1, list.items().size());
        assertEquals(existing.id(), list.items().get(0).id());
        assertEquals(5, list.items().get(0).quantity());
        assertEquals(existing.createdAt(), list.items().get(0).createdAt());
        assertEquals(LATER, list.updatedAt());
    }

    @Test
    void shouldChangeQuantityOfExistingProduct() {
        ShoppingList list = validList().items(List.of(milk(2))).build().changeQuantity(MILK_ID, 4, LATER);

        assertEquals(4, list.items().get(0).quantity());
        assertEquals(CREATED_AT, list.items().get(0).createdAt());
        assertEquals(LATER, list.updatedAt());
    }

    @Test
    void shouldRejectChangeQuantityWhenProductIsMissing() {
        assertThrows(
                InvalidShoppingListException.class, () -> validList().build().changeQuantity(MILK_ID, 1, LATER));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectAddProductWhenQuantityIsNotPositive(int quantity) {
        assertThrows(
                InvalidShoppingListItemException.class,
                () -> validList().build().addProduct(itemId(1), MILK_ID, quantity, LATER));
    }

    @Test
    void shouldRemoveProduct() {
        ShoppingList list = validList()
                .items(List.of(milk(1), bread(1)))
                .build()
                .removeProduct(MILK_ID, LATER);

        assertEquals(1, list.items().size());
        assertEquals(BREAD_ID, list.items().get(0).productId());
        assertEquals(LATER, list.updatedAt());
    }

    @Test
    void shouldRejectRemoveProductWhenProductIsMissing() {
        assertThrows(
                InvalidShoppingListException.class, () -> validList().build().removeProduct(MILK_ID, LATER));
    }

    @Test
    void shouldClearShoppingList() {
        ShoppingList list = validList().items(List.of(milk(2), bread(1))).build().clear(LATER);

        assertTrue(list.items().isEmpty());
        assertEquals(LIST_ID, list.id());
        assertEquals("Mercado semanal", list.name());
        assertEquals(CREATED_AT, list.createdAt());
        assertEquals(LATER, list.updatedAt());
    }

    @Test
    void shouldReconstituteShoppingListPreservingPersistedState() {
        List<ShoppingListItem> items = List.of(milk(2), bread(1));
        ShoppingList reconstituted = ShoppingList.reconstitute(
                LIST_ID, CUSTOMER_ID, "Mercado semanal", items, CREATED_AT, LATER);

        assertEquals(LIST_ID, reconstituted.id());
        assertEquals(CUSTOMER_ID, reconstituted.customerId());
        assertEquals("Mercado semanal", reconstituted.name());
        assertEquals(items, reconstituted.items());
        assertEquals(CREATED_AT, reconstituted.createdAt());
        assertEquals(LATER, reconstituted.updatedAt());
    }

    @Test
    void shouldRejectReconstituteWhenIdIsNull() {
        assertThrows(
                InvalidShoppingListException.class,
                () -> ShoppingList.reconstitute(null, CUSTOMER_ID, "Mercado semanal", List.of(), CREATED_AT, LATER));
    }

    @Test
    void shouldRejectReconstituteWhenNameIsBlank() {
        assertThrows(
                InvalidShoppingListException.class,
                () -> ShoppingList.reconstitute(LIST_ID, CUSTOMER_ID, "  ", List.of(), CREATED_AT, LATER));
    }

    @Test
    void shouldRejectReconstituteWhenCreatedAtIsAfterUpdatedAt() {
        assertThrows(
                InvalidShoppingListException.class,
                () -> ShoppingList.reconstitute(
                        LIST_ID, CUSTOMER_ID, "Mercado semanal", List.of(), LATER, CREATED_AT));
    }

    @Test
    void shouldRejectReconstituteWhenItemsContainDuplicateProductId() {
        assertThrows(
                InvalidShoppingListException.class,
                () -> ShoppingList.reconstitute(
                        LIST_ID, CUSTOMER_ID, "Mercado semanal", List.of(milk(1), milk(3)), CREATED_AT, LATER));
    }

    private static ShoppingListBuilder validList() {
        return new ShoppingListBuilder();
    }

    private static ShoppingListItem milk(int quantity) {
        return ShoppingListItem.create(itemId(1), MILK_ID, quantity, CREATED_AT);
    }

    private static ShoppingListItem bread(int quantity) {
        return ShoppingListItem.create(itemId(2), BREAD_ID, quantity, CREATED_AT);
    }

    private static UUID itemId(int suffix) {
        return UUID.fromString(String.format("99999999-9999-9999-9999-%012d", suffix));
    }

    private static final class ShoppingListBuilder {
        private UUID id = LIST_ID;
        private UUID customerId = CUSTOMER_ID;
        private String name = "Mercado semanal";
        private List<ShoppingListItem> items = List.of();
        private Instant createdAt = CREATED_AT;
        private Instant updatedAt = CREATED_AT;

        private ShoppingListBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        private ShoppingListBuilder customerId(UUID customerId) {
            this.customerId = customerId;
            return this;
        }

        private ShoppingListBuilder name(String name) {
            this.name = name;
            return this;
        }

        private ShoppingListBuilder items(List<ShoppingListItem> items) {
            this.items = items;
            return this;
        }

        private ShoppingListBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        private ShoppingListBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        private ShoppingList build() {
            return ShoppingList.create(id, customerId, name, items, createdAt, updatedAt);
        }
    }
}
