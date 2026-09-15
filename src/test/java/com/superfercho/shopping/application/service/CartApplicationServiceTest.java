package com.superfercho.shopping.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import com.superfercho.shopping.application.dto.cart.AddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.ChangeCartItemQuantityCommand;
import com.superfercho.shopping.application.dto.cart.ClearCartCommand;
import com.superfercho.shopping.application.dto.cart.GetCartQuery;
import com.superfercho.shopping.application.dto.cart.RemoveProductFromCartCommand;
import com.superfercho.shopping.application.exception.CartNotFoundException;
import com.superfercho.shopping.application.exception.DuplicateCustomerCartException;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.domain.exception.InvalidCartException;
import com.superfercho.shopping.domain.exception.InvalidCartItemException;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartItem;
import com.superfercho.shopping.domain.model.CartStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartApplicationServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-04-01T10:05:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MILK_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BREAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final Money MILK_PRICE = Money.cop(new BigDecimal("10.50"));
    private static final Money BREAD_PRICE = Money.cop(new BigDecimal("3.00"));

    @Mock
    private CartRepositoryPort cartRepository;

    @Mock
    private ProductCatalogPort productCatalogPort;

    @Mock
    private ClockPort clockPort;

    private CartApplicationService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartApplicationService(cartRepository, productCatalogPort, clockPort);
    }

    @Test
    void shouldGetExistingCart() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cartWithMilk()));

        CartResponse response = cartService.execute(new GetCartQuery(CUSTOMER_ID));

        assertEquals(CART_ID, response.id());
        assertEquals(CUSTOMER_ID, response.customerId());
        assertEquals(CartStatus.ACTIVE, response.status());
        assertEquals(1, response.items().size());
        assertEquals(MILK_ID, response.items().get(0).productId());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void shouldCreateCartWhenMissingOnGet() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(clockPort.currentTime()).thenReturn(NOW);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.execute(new GetCartQuery(CUSTOMER_ID));

        assertEquals(CUSTOMER_ID, response.customerId());
        assertEquals(CartStatus.ACTIVE, response.status());
        assertTrue(response.items().isEmpty());
        assertEquals(NOW, response.createdAt());
        assertEquals(NOW, response.updatedAt());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void shouldReturnWinnerCartWhenCreateLosesRaceOnGet() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(emptyCart()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(cartRepository.save(any(Cart.class))).thenThrow(new DuplicateCustomerCartException());

        CartResponse response = cartService.execute(new GetCartQuery(CUSTOMER_ID));

        assertEquals(CART_ID, response.id());
        assertEquals(CUSTOMER_ID, response.customerId());
        assertTrue(response.items().isEmpty());
        verify(cartRepository, times(2)).findByCustomerId(CUSTOMER_ID);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void shouldPropagateDuplicateCustomerCartWhenWinnerCartIsMissingOnGet() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(clockPort.currentTime()).thenReturn(NOW);
        when(cartRepository.save(any(Cart.class))).thenThrow(new DuplicateCustomerCartException());

        assertThrows(
                DuplicateCustomerCartException.class, () -> cartService.execute(new GetCartQuery(CUSTOMER_ID)));
        verify(cartRepository, times(2)).findByCustomerId(CUSTOMER_ID);
    }

    @Test
    void shouldAddNewProductToExistingCart() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(emptyCart()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(productCatalogPort.getProduct(MILK_ID)).thenReturn(Optional.of(new ProductCatalogInfo(MILK_ID, MILK_PRICE)));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response =
                cartService.execute(new AddProductToCartCommand(CUSTOMER_ID, MILK_ID, 2));

        assertEquals(1, response.items().size());
        assertEquals(MILK_ID, response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(MILK_PRICE, response.items().get(0).priceAtAddition());
        assertEquals(NOW, response.updatedAt());
        verify(productCatalogPort).getProduct(MILK_ID);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void shouldIncrementQuantityWhenProductAlreadyInCart() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cartWithMilk()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(productCatalogPort.getProduct(MILK_ID))
                .thenReturn(Optional.of(new ProductCatalogInfo(MILK_ID, Money.cop(new BigDecimal("99.00")))));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response =
                cartService.execute(new AddProductToCartCommand(CUSTOMER_ID, MILK_ID, 3));

        assertEquals(1, response.items().size());
        assertEquals(ITEM_ID, response.items().get(0).id());
        assertEquals(5, response.items().get(0).quantity());
        assertEquals(MILK_PRICE, response.items().get(0).priceAtAddition());
        assertEquals(NOW, response.updatedAt());
        verify(productCatalogPort).getProduct(MILK_ID);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void shouldAddProductCreatingCartWhenMissing() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(clockPort.currentTime()).thenReturn(NOW);
        when(productCatalogPort.getProduct(MILK_ID)).thenReturn(Optional.of(new ProductCatalogInfo(MILK_ID, MILK_PRICE)));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response =
                cartService.execute(new AddProductToCartCommand(CUSTOMER_ID, MILK_ID, 1));

        assertEquals(1, response.items().size());
        assertEquals(MILK_ID, response.items().get(0).productId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void shouldReapplyAddProductWhenInitialCartCreateLosesRace() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(emptyCart()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(productCatalogPort.getProduct(MILK_ID)).thenReturn(Optional.of(new ProductCatalogInfo(MILK_ID, MILK_PRICE)));
        when(cartRepository.save(any(Cart.class)))
                .thenThrow(new DuplicateCustomerCartException())
                .thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response =
                cartService.execute(new AddProductToCartCommand(CUSTOMER_ID, MILK_ID, 2));

        assertEquals(CART_ID, response.id());
        assertEquals(1, response.items().size());
        assertEquals(MILK_ID, response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(MILK_PRICE, response.items().get(0).priceAtAddition());

        ArgumentCaptor<Cart> savedCarts = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository, times(2)).save(savedCarts.capture());
        Cart retriedSave = savedCarts.getAllValues().get(1);
        assertEquals(CART_ID, retriedSave.id());
        assertEquals(1, retriedSave.items().size());
        assertEquals(MILK_ID, retriedSave.items().get(0).productId());
        assertEquals(2, retriedSave.items().get(0).quantity());
        verify(cartRepository, times(2)).findByCustomerId(CUSTOMER_ID);
    }

    @Test
    void shouldChangeCartItemQuantity() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cartWithMilk()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response =
                cartService.execute(new ChangeCartItemQuantityCommand(CUSTOMER_ID, MILK_ID, 4));

        assertEquals(4, response.items().get(0).quantity());
        assertEquals(NOW, response.updatedAt());
        verify(cartRepository).save(any(Cart.class));
        verify(productCatalogPort, never()).getProduct(any());
    }

    @Test
    void shouldRemoveProductFromCart() {
        Cart cart = emptyCart().addProduct(ITEM_ID, MILK_ID, 1, MILK_PRICE, CREATED_AT)
                .addProduct(UUID.fromString("99999999-9999-9999-9999-000000000002"), BREAD_ID, 1, BREAD_PRICE, CREATED_AT);
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cart));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.execute(new RemoveProductFromCartCommand(CUSTOMER_ID, MILK_ID));

        assertEquals(1, response.items().size());
        assertEquals(BREAD_ID, response.items().get(0).productId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void shouldClearCart() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cartWithMilk()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.execute(new ClearCartCommand(CUSTOMER_ID));

        assertTrue(response.items().isEmpty());
        assertEquals(CART_ID, response.id());
        assertEquals(NOW, response.updatedAt());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void shouldRejectChangeQuantityWhenCartDoesNotExist() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThrows(
                CartNotFoundException.class,
                () -> cartService.execute(new ChangeCartItemQuantityCommand(CUSTOMER_ID, MILK_ID, 1)));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void shouldRejectAddWhenProductDoesNotExist() {
        when(productCatalogPort.getProduct(MILK_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> cartService.execute(new AddProductToCartCommand(CUSTOMER_ID, MILK_ID, 1)));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void shouldPropagateDomainExceptionWhenQuantityIsInvalid() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cartWithMilk()));
        when(clockPort.currentTime()).thenReturn(NOW);

        assertThrows(
                InvalidCartItemException.class,
                () -> cartService.execute(new ChangeCartItemQuantityCommand(CUSTOMER_ID, MILK_ID, 0)));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void shouldPropagateDomainExceptionWhenRemovingMissingProduct() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(emptyCart()));
        when(clockPort.currentTime()).thenReturn(NOW);

        assertThrows(
                InvalidCartException.class,
                () -> cartService.execute(new RemoveProductFromCartCommand(CUSTOMER_ID, MILK_ID)));
        verify(cartRepository, never()).save(any());
    }

    private Cart emptyCart() {
        return Cart.create(CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), CREATED_AT, CREATED_AT);
    }

    private Cart cartWithMilk() {
        return Cart.create(
                CART_ID,
                CUSTOMER_ID,
                CartStatus.ACTIVE,
                List.of(CartItem.create(ITEM_ID, MILK_ID, 2, MILK_PRICE, CREATED_AT, CREATED_AT)),
                CREATED_AT,
                CREATED_AT);
    }
}
