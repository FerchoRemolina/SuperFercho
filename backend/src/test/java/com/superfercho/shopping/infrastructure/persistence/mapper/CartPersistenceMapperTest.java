package com.superfercho.shopping.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartItem;
import com.superfercho.shopping.domain.model.CartStatus;
import com.superfercho.shopping.infrastructure.persistence.entity.CartItemJpaEntity;
import com.superfercho.shopping.infrastructure.persistence.entity.CartJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartPersistenceMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:20:00Z");
    private static final Instant ADDED_AT = Instant.parse("2026-03-01T10:05:00Z");
    private static final Instant ITEM_UPDATED_AT = Instant.parse("2026-03-01T10:15:00Z");
    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SECOND_PRODUCT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID SECOND_ITEM_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private final CartPersistenceMapper mapper = new CartPersistenceMapper();

    @Test
    void shouldMapEmptyCartRoundTrip() {
        Cart cart = Cart.reconstitute(CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), CREATED_AT, UPDATED_AT);

        Cart mapped = mapper.toDomain(mapper.toEntity(cart));

        assertEquals(CART_ID, mapped.id());
        assertEquals(CUSTOMER_ID, mapped.customerId());
        assertEquals(CartStatus.ACTIVE, mapped.status());
        assertTrue(mapped.items().isEmpty());
        assertEquals(CREATED_AT, mapped.createdAt());
        assertEquals(UPDATED_AT, mapped.updatedAt());
    }

    @Test
    void shouldMapCartWithItemsRoundTrip() {
        Cart cart = cartWithItems();

        CartJpaEntity entity = mapper.toEntity(cart);
        Cart mapped = mapper.toDomain(entity);

        assertEquals(CART_ID, entity.getId());
        assertEquals(CUSTOMER_ID, entity.getCustomerId());
        assertEquals(CartStatus.ACTIVE, entity.getStatus());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
        assertEquals(2, entity.getItems().size());

        CartItemJpaEntity first = entity.getItems().get(0);
        assertEquals(ITEM_ID, first.getId());
        assertEquals(PRODUCT_ID, first.getProductId());
        assertEquals(2, first.getQuantity());
        assertEquals(new BigDecimal("10.50"), first.getPriceAtAdditionAmount());
        assertEquals(Money.COP, first.getPriceAtAdditionCurrency());
        assertEquals(ADDED_AT, first.getAddedAt());
        assertEquals(ITEM_UPDATED_AT, first.getUpdatedAt());

        assertEquals(cart.id(), mapped.id());
        assertEquals(cart.customerId(), mapped.customerId());
        assertEquals(CartStatus.ACTIVE, mapped.status());
        assertEquals(cart.items(), mapped.items());
        assertEquals(CREATED_AT, mapped.createdAt());
        assertEquals(UPDATED_AT, mapped.updatedAt());
        assertEquals(PRODUCT_ID, mapped.items().get(0).productId());
        assertEquals(2, mapped.items().get(0).quantity());
        assertEquals(Money.cop(new BigDecimal("10.50")), mapped.items().get(0).priceAtAddition());
        assertEquals(Money.COP, mapped.items().get(0).priceAtAddition().currency());
        assertEquals(ADDED_AT, mapped.items().get(0).addedAt());
        assertEquals(ITEM_UPDATED_AT, mapped.items().get(0).updatedAt());
    }

    @Test
    void shouldPreserveItemOrder() {
        Cart cart = cartWithItems();

        Cart mapped = mapper.toDomain(mapper.toEntity(cart));

        assertEquals(ITEM_ID, mapped.items().get(0).id());
        assertEquals(SECOND_ITEM_ID, mapped.items().get(1).id());
        assertEquals(PRODUCT_ID, mapped.items().get(0).productId());
        assertEquals(SECOND_PRODUCT_ID, mapped.items().get(1).productId());
        assertEquals(2, mapped.items().get(0).quantity());
        assertEquals(1, mapped.items().get(1).quantity());
    }

    private static Cart cartWithItems() {
        return Cart.reconstitute(
                CART_ID,
                CUSTOMER_ID,
                CartStatus.ACTIVE,
                List.of(milk(2), bread(1)),
                CREATED_AT,
                UPDATED_AT);
    }

    private static CartItem milk(int quantity) {
        return CartItem.create(
                ITEM_ID,
                PRODUCT_ID,
                quantity,
                Money.cop(new BigDecimal("10.50")),
                ADDED_AT,
                ITEM_UPDATED_AT);
    }

    private static CartItem bread(int quantity) {
        return CartItem.create(
                SECOND_ITEM_ID,
                SECOND_PRODUCT_ID,
                quantity,
                Money.cop(new BigDecimal("3.00")),
                ADDED_AT,
                ADDED_AT);
    }
}
