package com.superfercho.shopping.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.usecase.FindProductPriceUseCase;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.cart.AddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartStatus;
import com.superfercho.shopping.domain.model.ShoppingList;
import com.superfercho.shopping.infrastructure.catalog.ProductCatalogAdapter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InactiveProductShoppingAvailabilityTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-04-01T10:05:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID LIST_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CATEGORY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private CartRepositoryPort cartRepository;

    @Mock
    private ShoppingListRepositoryPort shoppingListRepository;

    @Mock
    private ClockPort clockPort;

    @Mock
    private ProductRepository productRepository;

    private CartApplicationService cartService;
    private ShoppingListApplicationService shoppingListService;

    @BeforeEach
    void setUp() {
        ProductCatalogAdapter catalog = new ProductCatalogAdapter(new FindProductPriceUseCase(productRepository));
        cartService = new CartApplicationService(
                currentUserProvider, cartRepository, shoppingListRepository, catalog, clockPort);
        shoppingListService = new ShoppingListApplicationService(
                currentUserProvider, shoppingListRepository, catalog, clockPort);
    }

    @Test
    void shouldAddActiveProductToCart() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(emptyCart()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(ProductStatus.ACTIVE)));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.execute(new AddProductToCartCommand(PRODUCT_ID, 2));

        assertEquals(1, response.items().size());
        assertEquals(PRODUCT_ID, response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(PRICE, response.items().get(0).priceAtAddition());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void shouldRejectInactiveProductWhenAddingToCart() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(ProductStatus.INACTIVE)));

        assertThrows(
                ProductNotFoundException.class, () -> cartService.execute(new AddProductToCartCommand(PRODUCT_ID, 1)));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void shouldAddActiveProductToShoppingList() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(emptyList()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(ProductStatus.ACTIVE)));
        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingListResponse response =
                shoppingListService.execute(new AddProductToShoppingListCommand(LIST_ID, PRODUCT_ID, 2));

        assertEquals(1, response.items().size());
        assertEquals(PRODUCT_ID, response.items().get(0).productId());
        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void shouldRejectInactiveProductWhenAddingToShoppingList() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(ProductStatus.INACTIVE)));

        assertThrows(
                ProductNotFoundException.class,
                () -> shoppingListService.execute(new AddProductToShoppingListCommand(LIST_ID, PRODUCT_ID, 1)));
        verify(shoppingListRepository, never()).save(any());
    }

    private Cart emptyCart() {
        return Cart.create(CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), CREATED_AT, CREATED_AT);
    }

    private ShoppingList emptyList() {
        return ShoppingList.create(LIST_ID, CUSTOMER_ID, "Mercado semanal", List.of(), CREATED_AT, CREATED_AT);
    }

    private static Product product(ProductStatus status) {
        return Product.create(
                PRODUCT_ID,
                CATEGORY_ID,
                "7701234567890",
                "Leche entera",
                "Alpina",
                "1L",
                PRICE,
                10,
                null,
                status,
                CREATED_AT,
                CREATED_AT);
    }
}
