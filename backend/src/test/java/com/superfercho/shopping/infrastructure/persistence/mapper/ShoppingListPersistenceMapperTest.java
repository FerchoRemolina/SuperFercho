package com.superfercho.shopping.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.shopping.domain.model.ShoppingList;
import com.superfercho.shopping.domain.model.ShoppingListItem;
import com.superfercho.shopping.infrastructure.persistence.entity.ShoppingListItemJpaEntity;
import com.superfercho.shopping.infrastructure.persistence.entity.ShoppingListJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShoppingListPersistenceMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:20:00Z");
    private static final Instant ITEM_CREATED_AT = Instant.parse("2026-03-01T10:05:00Z");
    private static final Instant SECOND_ITEM_CREATED_AT = Instant.parse("2026-03-01T10:10:00Z");
    private static final UUID LIST_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SECOND_PRODUCT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID SECOND_ITEM_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private final ShoppingListPersistenceMapper mapper = new ShoppingListPersistenceMapper();

    @Test
    void shouldMapEmptyShoppingListRoundTrip() {
        ShoppingList list =
                ShoppingList.reconstitute(LIST_ID, CUSTOMER_ID, "Weekly groceries", List.of(), CREATED_AT, UPDATED_AT);

        ShoppingList mapped = mapper.toDomain(mapper.toEntity(list));

        assertEquals(LIST_ID, mapped.id());
        assertEquals(CUSTOMER_ID, mapped.customerId());
        assertEquals("Weekly groceries", mapped.name());
        assertTrue(mapped.items().isEmpty());
        assertEquals(CREATED_AT, mapped.createdAt());
        assertEquals(UPDATED_AT, mapped.updatedAt());
    }

    @Test
    void shouldMapShoppingListWithItemsRoundTrip() {
        ShoppingList list = listWithItems();

        ShoppingListJpaEntity entity = mapper.toEntity(list);
        ShoppingList mapped = mapper.toDomain(entity);

        assertEquals(LIST_ID, entity.getId());
        assertEquals(CUSTOMER_ID, entity.getCustomerId());
        assertEquals("Weekly groceries", entity.getName());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
        assertEquals(2, entity.getItems().size());

        ShoppingListItemJpaEntity first = entity.getItems().get(0);
        assertEquals(ITEM_ID, first.getId());
        assertEquals(PRODUCT_ID, first.getProductId());
        assertEquals(3, first.getQuantity());
        assertEquals(ITEM_CREATED_AT, first.getCreatedAt());

        assertEquals(list.id(), mapped.id());
        assertEquals(list.customerId(), mapped.customerId());
        assertEquals(list.name(), mapped.name());
        assertEquals(list.items(), mapped.items());
        assertEquals(CREATED_AT, mapped.createdAt());
        assertEquals(UPDATED_AT, mapped.updatedAt());
        assertEquals(PRODUCT_ID, mapped.items().get(0).productId());
        assertEquals(3, mapped.items().get(0).quantity());
        assertEquals(ITEM_CREATED_AT, mapped.items().get(0).createdAt());
    }

    @Test
    void shouldPreserveItemOrder() {
        ShoppingList mapped = mapper.toDomain(mapper.toEntity(listWithItems()));

        assertEquals(ITEM_ID, mapped.items().get(0).id());
        assertEquals(SECOND_ITEM_ID, mapped.items().get(1).id());
        assertEquals(PRODUCT_ID, mapped.items().get(0).productId());
        assertEquals(SECOND_PRODUCT_ID, mapped.items().get(1).productId());
        assertEquals(3, mapped.items().get(0).quantity());
        assertEquals(1, mapped.items().get(1).quantity());
        assertEquals(ITEM_CREATED_AT, mapped.items().get(0).createdAt());
        assertEquals(SECOND_ITEM_CREATED_AT, mapped.items().get(1).createdAt());
    }

    private static ShoppingList listWithItems() {
        return ShoppingList.reconstitute(
                LIST_ID,
                CUSTOMER_ID,
                "Weekly groceries",
                List.of(milk(3), bread(1)),
                CREATED_AT,
                UPDATED_AT);
    }

    private static ShoppingListItem milk(int quantity) {
        return ShoppingListItem.create(ITEM_ID, PRODUCT_ID, quantity, ITEM_CREATED_AT);
    }

    private static ShoppingListItem bread(int quantity) {
        return ShoppingListItem.create(SECOND_ITEM_ID, SECOND_PRODUCT_ID, quantity, SECOND_ITEM_CREATED_AT);
    }
}
