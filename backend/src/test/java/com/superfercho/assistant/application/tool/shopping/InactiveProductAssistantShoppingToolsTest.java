package com.superfercho.assistant.application.tool.shopping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.application.usecase.FindProductPriceUseCase;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.application.service.CartApplicationService;
import com.superfercho.shopping.application.service.ShoppingListApplicationService;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartStatus;
import com.superfercho.shopping.infrastructure.catalog.ProductCatalogAdapter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InactiveProductAssistantShoppingToolsTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-04-01T10:05:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID LIST_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CATEGORY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID TYPE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Presentation UNIT = Presentation.of(1, PresentationUnit.UNIT);
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

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductTypeRepository productTypeRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        ProductCatalogAdapter catalog = new ProductCatalogAdapter(new FindProductPriceUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository));
        CartApplicationService cartService = new CartApplicationService(
                currentUserProvider, cartRepository, shoppingListRepository, catalog, clockPort);
        ShoppingListApplicationService shoppingListService = new ShoppingListApplicationService(
                currentUserProvider, shoppingListRepository, catalog, clockPort);
        registry = new ToolRegistry(List.of(new AddCartItemTool(cartService), new AddShoppingListItemTool(shoppingListService)));
    }

    @Test
    void shouldAddActiveProductWithActiveCategoryToCartThroughAssistantTool() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(emptyCart()));
        when(clockPort.currentTime()).thenReturn(NOW);
        givenProduct(ProductStatus.ACTIVE, CategoryStatus.ACTIVE);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ToolResult result = registry.execute(
                ToolNames.ADD_CART_ITEM, Map.of("productId", PRODUCT_ID.toString(), "quantity", 2));

        assertTrue(result.success());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void shouldNotAddInactiveProductToCartThroughAssistantTool() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        givenProduct(ProductStatus.INACTIVE, CategoryStatus.ACTIVE);

        ToolResult result = registry.execute(
                ToolNames.ADD_CART_ITEM, Map.of("productId", PRODUCT_ID.toString(), "quantity", 1));

        assertFalse(result.success());
        assertEquals(new ProductNotFoundException(PRODUCT_ID).getMessage(), result.content());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void shouldNotAddProductWithInactiveCategoryToCartThroughAssistantTool() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        givenProduct(ProductStatus.ACTIVE, CategoryStatus.INACTIVE);

        ToolResult result = registry.execute(
                ToolNames.ADD_CART_ITEM, Map.of("productId", PRODUCT_ID.toString(), "quantity", 1));

        assertFalse(result.success());
        assertEquals(new ProductNotFoundException(PRODUCT_ID).getMessage(), result.content());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void shouldNotAddInactiveProductToShoppingListThroughAssistantTool() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        givenProduct(ProductStatus.INACTIVE, CategoryStatus.ACTIVE);

        ToolResult result = registry.execute(
                ToolNames.ADD_SHOPPING_LIST_ITEM,
                Map.of(
                        "shoppingListId",
                        LIST_ID.toString(),
                        "productId",
                        PRODUCT_ID.toString(),
                        "quantity",
                        1));

        assertFalse(result.success());
        assertEquals(new ProductNotFoundException(PRODUCT_ID).getMessage(), result.content());
        verify(shoppingListRepository, never()).save(any());
    }

    private void givenProduct(ProductStatus productStatus, CategoryStatus categoryStatus) {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(productStatus)));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category(categoryStatus)));
        when(productTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(ProductType.create(
                        TYPE_ID, CATEGORY_ID, "Leche", null, ProductTypeStatus.ACTIVE, CREATED_AT, CREATED_AT)));
    }

    private Cart emptyCart() {
        return Cart.create(CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), CREATED_AT, CREATED_AT);
    }

    private static Category category(CategoryStatus status) {
        return Category.create(CATEGORY_ID, "Lácteos", null, status, CREATED_AT, CREATED_AT);
    }

    private static Product product(ProductStatus status) {
        return Product.create(
                PRODUCT_ID,
                CATEGORY_ID,
                TYPE_ID,
                null,
                UNIT,
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
