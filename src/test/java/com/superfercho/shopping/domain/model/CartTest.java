package com.superfercho.shopping.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.domain.exception.InvalidCartException;
import com.superfercho.shopping.domain.exception.InvalidCartItemException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CartTest {

    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID MILK_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BREAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Instant CREATED_AT = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-04-01T10:05:00Z");
    private static final Money MILK_PRICE = Money.cop(new BigDecimal("10.50"));
    private static final Money BREAD_PRICE = Money.cop(new BigDecimal("3.00"));

    @Test
    void shouldCreateEmptyActiveCart() {
        Cart cart = validCart().build();

        assertEquals(CART_ID, cart.id());
        assertEquals(CUSTOMER_ID, cart.customerId());
        assertEquals(CartStatus.ACTIVE, cart.status());
        assertTrue(cart.items().isEmpty());
        assertEquals(CREATED_AT, cart.createdAt());
        assertEquals(CREATED_AT, cart.updatedAt());
    }

    @Test
    void shouldCreateCartWithItems() {
        CartItem milk = milk(2);
        Cart cart = validCart().items(List.of(milk)).build();

        assertEquals(1, cart.items().size());
        assertEquals(milk, cart.items().get(0));
    }

    @Test
    void shouldRejectCartWhenIdIsNull() {
        assertThrows(InvalidCartException.class, () -> validCart().id(null).build());
    }

    @Test
    void shouldRejectCartWhenCustomerIdIsNull() {
        assertThrows(InvalidCartException.class, () -> validCart().customerId(null).build());
    }

    @Test
    void shouldRejectCartWhenStatusIsNull() {
        assertThrows(InvalidCartException.class, () -> validCart().status(null).build());
    }

    @Test
    void shouldRejectCartWhenCreatedAtIsNull() {
        assertThrows(InvalidCartException.class, () -> validCart().createdAt(null).build());
    }

    @Test
    void shouldRejectCartWhenUpdatedAtIsNull() {
        assertThrows(InvalidCartException.class, () -> validCart().updatedAt(null).build());
    }

    @Test
    void shouldRejectCartWhenCreatedAtIsAfterUpdatedAt() {
        assertThrows(InvalidCartException.class, () -> validCart().updatedAt(CREATED_AT.minusSeconds(1)).build());
    }

    @Test
    void shouldRejectCartWhenItemsIsNull() {
        assertThrows(InvalidCartException.class, () -> validCart().items(null).build());
    }

    @Test
    void shouldRejectCartWhenItemsContainNull() {
        List<CartItem> items = new ArrayList<>();
        items.add(null);
        assertThrows(InvalidCartException.class, () -> validCart().items(items).build());
    }

    @Test
    void shouldRejectCartWhenItemsContainDuplicateProductId() {
        assertThrows(
                InvalidCartException.class,
                () -> validCart().items(List.of(milk(1), milk(2))).build());
    }

    @Test
    void shouldKeepItemSnapshotIndependentFromOriginalList() {
        CartItem item = milk(1);
        List<CartItem> items = new ArrayList<>();
        items.add(item);

        Cart cart = validCart().items(items).build();
        items.clear();

        assertEquals(1, cart.items().size());
        assertEquals(item, cart.items().get(0));
    }

    @Test
    void shouldExposeItemsAsUnmodifiableCollection() {
        Cart cart = validCart().items(List.of(milk(1))).build();

        assertThrows(UnsupportedOperationException.class, () -> cart.items().add(bread(1)));
        assertThrows(UnsupportedOperationException.class, () -> cart.items().clear());
    }

    @Test
    void shouldAddProductToEmptyCart() {
        Cart cart = validCart().build().addProduct(itemId(1), MILK_ID, 2, MILK_PRICE, LATER);

        assertEquals(1, cart.items().size());
        assertEquals(MILK_ID, cart.items().get(0).productId());
        assertEquals(2, cart.items().get(0).quantity());
        assertEquals(MILK_PRICE, cart.items().get(0).priceAtAddition());
        assertEquals(LATER, cart.items().get(0).addedAt());
        assertEquals(LATER, cart.updatedAt());
        assertEquals(CREATED_AT, cart.createdAt());
    }

    @Test
    void shouldIncrementQuantityWhenProductAlreadyExists() {
        CartItem existing = milk(2);
        Cart cart = validCart()
                .items(List.of(existing))
                .build()
                .addProduct(itemId(99), MILK_ID, 3, Money.cop(new BigDecimal("99.00")), LATER);

        assertEquals(1, cart.items().size());
        assertEquals(existing.id(), cart.items().get(0).id());
        assertEquals(5, cart.items().get(0).quantity());
        assertEquals(MILK_PRICE, cart.items().get(0).priceAtAddition());
        assertEquals(existing.addedAt(), cart.items().get(0).addedAt());
        assertEquals(LATER, cart.items().get(0).updatedAt());
    }

    @Test
    void shouldAddSecondProductWithoutMerging() {
        Cart cart = validCart()
                .items(List.of(milk(1)))
                .build()
                .addProduct(itemId(2), BREAD_ID, 1, BREAD_PRICE, LATER);

        assertEquals(2, cart.items().size());
        assertEquals(MILK_ID, cart.items().get(0).productId());
        assertEquals(BREAD_ID, cart.items().get(1).productId());
    }

    @Test
    void shouldChangeQuantityOfExistingProduct() {
        Cart cart = validCart().items(List.of(milk(2))).build().changeQuantity(MILK_ID, 4, LATER);

        assertEquals(4, cart.items().get(0).quantity());
        assertEquals(LATER, cart.updatedAt());
        assertEquals(MILK_PRICE, cart.items().get(0).priceAtAddition());
    }

    @Test
    void shouldRejectChangeQuantityWhenProductIsMissing() {
        assertThrows(
                InvalidCartException.class, () -> validCart().build().changeQuantity(MILK_ID, 1, LATER));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectAddProductWhenQuantityIsNotPositive(int quantity) {
        assertThrows(
                InvalidCartItemException.class,
                () -> validCart().build().addProduct(itemId(1), MILK_ID, quantity, MILK_PRICE, LATER));
    }

    @Test
    void shouldRemoveProduct() {
        Cart cart = validCart()
                .items(List.of(milk(1), bread(1)))
                .build()
                .removeProduct(MILK_ID, LATER);

        assertEquals(1, cart.items().size());
        assertEquals(BREAD_ID, cart.items().get(0).productId());
        assertEquals(LATER, cart.updatedAt());
    }

    @Test
    void shouldRejectRemoveProductWhenProductIsMissing() {
        assertThrows(InvalidCartException.class, () -> validCart().build().removeProduct(MILK_ID, LATER));
    }

    @Test
    void shouldClearCart() {
        Cart cart = validCart().items(List.of(milk(2), bread(1))).build().clear(LATER);

        assertTrue(cart.items().isEmpty());
        assertEquals(CART_ID, cart.id());
        assertEquals(CartStatus.ACTIVE, cart.status());
        assertEquals(CREATED_AT, cart.createdAt());
        assertEquals(LATER, cart.updatedAt());
    }

    @Test
    void shouldRejectOperationWhenCurrentTimeIsBeforeCreatedAt() {
        Cart cart = validCart().build();

        assertThrows(
                InvalidCartException.class,
                () -> cart.addProduct(itemId(1), MILK_ID, 1, MILK_PRICE, CREATED_AT.minusSeconds(1)));
    }

    @Test
    void shouldReconstituteCartPreservingPersistedState() {
        List<CartItem> items = List.of(milk(2), bread(1));
        Cart reconstituted = Cart.reconstitute(
                CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, items, CREATED_AT, LATER);

        assertEquals(CART_ID, reconstituted.id());
        assertEquals(CUSTOMER_ID, reconstituted.customerId());
        assertEquals(CartStatus.ACTIVE, reconstituted.status());
        assertEquals(items, reconstituted.items());
        assertEquals(CREATED_AT, reconstituted.createdAt());
        assertEquals(LATER, reconstituted.updatedAt());
    }

    @Test
    void shouldRejectReconstituteWhenIdIsNull() {
        assertThrows(
                InvalidCartException.class,
                () -> Cart.reconstitute(null, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), CREATED_AT, LATER));
    }

    @Test
    void shouldRejectReconstituteWhenCreatedAtIsAfterUpdatedAt() {
        assertThrows(
                InvalidCartException.class,
                () -> Cart.reconstitute(
                        CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), LATER, CREATED_AT));
    }

    @Test
    void shouldRejectReconstituteWhenItemsContainDuplicateProductId() {
        assertThrows(
                InvalidCartException.class,
                () -> Cart.reconstitute(
                        CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(milk(1), milk(2)), CREATED_AT, LATER));
    }

    private static CartBuilder validCart() {
        return new CartBuilder();
    }

    private static CartItem milk(int quantity) {
        return CartItem.create(itemId(1), MILK_ID, quantity, MILK_PRICE, CREATED_AT, CREATED_AT);
    }

    private static CartItem bread(int quantity) {
        return CartItem.create(itemId(2), BREAD_ID, quantity, BREAD_PRICE, CREATED_AT, CREATED_AT);
    }

    private static UUID itemId(int suffix) {
        return UUID.fromString(String.format("99999999-9999-9999-9999-%012d", suffix));
    }

    private static final class CartBuilder {
        private UUID id = CART_ID;
        private UUID customerId = CUSTOMER_ID;
        private CartStatus status = CartStatus.ACTIVE;
        private List<CartItem> items = List.of();
        private Instant createdAt = CREATED_AT;
        private Instant updatedAt = CREATED_AT;

        private CartBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        private CartBuilder customerId(UUID customerId) {
            this.customerId = customerId;
            return this;
        }

        private CartBuilder status(CartStatus status) {
            this.status = status;
            return this;
        }

        private CartBuilder items(List<CartItem> items) {
            this.items = items;
            return this;
        }

        private CartBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        private CartBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        private Cart build() {
            return Cart.create(id, customerId, status, items, createdAt, updatedAt);
        }
    }
}
