package com.superfercho.orders.infrastructure.shopping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.CartSnapshot;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.cart.CartItemResponse;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.ClearCartCommand;
import com.superfercho.shopping.application.exception.CartNotFoundException;
import com.superfercho.shopping.application.port.in.ClearCartUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.domain.model.CartStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingCartAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MILK_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BREAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID MILK_ITEM_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID BREAD_ITEM_ID = UUID.fromString("99999999-9999-9999-9999-000000000002");
    private static final Money MILK_PRICE = Money.cop(new BigDecimal("10.50"));
    private static final Money BREAD_PRICE = Money.cop(new BigDecimal("4.00"));

    @Mock
    private GetCartUseCase getCartUseCase;

    @Mock
    private ClearCartUseCase clearCartUseCase;

    private ShoppingCartAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShoppingCartAdapter(getCartUseCase, clearCartUseCase);
    }

    @Test
    void shouldDelegateGetActiveCartToGetCartUseCase() {
        when(getCartUseCase.execute()).thenReturn(cartWithItems());

        adapter.getActiveCart(CUSTOMER_ID);

        verify(getCartUseCase).execute();
    }

    @Test
    void shouldMapCartResponseToCartSnapshot() {
        when(getCartUseCase.execute()).thenReturn(cartWithItems());

        CartSnapshot snapshot = adapter.getActiveCart(CUSTOMER_ID);

        assertEquals(CART_ID, snapshot.cartId());
        assertEquals(2, snapshot.items().size());
        assertEquals(MILK_ID, snapshot.items().get(0).productId());
        assertEquals(2, snapshot.items().get(0).quantity());
        assertEquals(MILK_PRICE, snapshot.items().get(0).priceAtAddition());
        assertEquals(BREAD_ID, snapshot.items().get(1).productId());
        assertEquals(1, snapshot.items().get(1).quantity());
        assertEquals(BREAD_PRICE, snapshot.items().get(1).priceAtAddition());
    }

    @Test
    void shouldMapEmptyCartWithoutInventingItems() {
        when(getCartUseCase.execute()).thenReturn(emptyCart());

        CartSnapshot snapshot = adapter.getActiveCart(CUSTOMER_ID);

        assertEquals(CART_ID, snapshot.cartId());
        assertTrue(snapshot.isEmpty());
    }

    @Test
    void shouldDelegateClearCartAndIgnoreReturnedCart() {
        when(clearCartUseCase.execute(any(ClearCartCommand.class))).thenReturn(emptyCart());

        adapter.clearCart(CUSTOMER_ID);

        verify(clearCartUseCase).execute(new ClearCartCommand());
    }

    @Test
    void shouldPropagateCartNotFoundFromClearCart() {
        CartNotFoundException missing = new CartNotFoundException(CUSTOMER_ID);
        when(clearCartUseCase.execute(any(ClearCartCommand.class))).thenThrow(missing);

        CartNotFoundException thrown =
                assertThrows(CartNotFoundException.class, () -> adapter.clearCart(CUSTOMER_ID));
        assertSame(missing, thrown);
    }

    private static CartResponse cartWithItems() {
        return new CartResponse(
                CART_ID,
                CUSTOMER_ID,
                CartStatus.ACTIVE,
                List.of(
                        new CartItemResponse(MILK_ITEM_ID, MILK_ID, 2, MILK_PRICE, NOW, NOW),
                        new CartItemResponse(BREAD_ITEM_ID, BREAD_ID, 1, BREAD_PRICE, NOW, NOW)),
                NOW,
                NOW);
    }

    private static CartResponse emptyCart() {
        return new CartResponse(CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), NOW, NOW);
    }
}
