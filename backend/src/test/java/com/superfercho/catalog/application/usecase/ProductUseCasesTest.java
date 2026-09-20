package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ActivateProductCommand;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ChangeProductPriceCommand;
import com.superfercho.catalog.application.dto.CreateProductCommand;
import com.superfercho.catalog.application.dto.DeactivateProductCommand;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
import com.superfercho.catalog.application.dto.UpdateProductCommand;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Money PRICE = Money.cop(new BigDecimal("4500.00"));

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private CreateProductUseCase createProduct;
    private UpdateProductUseCase updateProduct;
    private GetProductUseCase getProduct;
    private ListProductsUseCase listProducts;
    private SearchProductsUseCase searchProducts;
    private ActivateProductUseCase activateProduct;
    private DeactivateProductUseCase deactivateProduct;
    private ChangeProductPriceUseCase changeProductPrice;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        createProduct = new CreateProductUseCase(productRepository, categoryRepository, clock);
        updateProduct = new UpdateProductUseCase(productRepository, categoryRepository, clock);
        getProduct = new GetProductUseCase(productRepository, categoryRepository);
        listProducts = new ListProductsUseCase(productRepository, categoryRepository);
        searchProducts = new SearchProductsUseCase(productRepository, categoryRepository);
        activateProduct = new ActivateProductUseCase(productRepository, clock);
        deactivateProduct = new DeactivateProductUseCase(productRepository, clock);
        changeProductPrice = new ChangeProductPriceUseCase(productRepository, clock);
    }

    @Test
    void shouldCreateProductAsActive() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(activeCategory()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = createProduct.execute(createCommand());

        assertEquals(CATEGORY_ID, result.categoryId());
        assertEquals("Leche entera", result.name());
        assertEquals(PRICE, result.price());
        assertEquals(10, result.stock());
        assertEquals(ProductStatus.ACTIVE, result.status());
        assertEquals(NOW, result.createdAt());
        verify(productRepository).save(any(Product.class));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenCategoryIsMissing() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(InvalidCategoryReferenceException.class, () -> createProduct.execute(createCommand()));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectGetWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> getProduct.execute(new GetProductCommand(PRODUCT_ID, CatalogView.ADMIN)));
    }

    @Test
    void shouldRejectUpdateWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> updateProduct.execute(updateCommand()));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectActivateWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> activateProduct.execute(new ActivateProductCommand(PRODUCT_ID)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectDeactivateWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> deactivateProduct.execute(new DeactivateProductCommand(PRODUCT_ID)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectPriceChangeWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> changeProductPrice.execute(
                        new ChangeProductPriceCommand(PRODUCT_ID, Money.cop(new BigDecimal("5000.00")))));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldUpdateProductWithoutChangingPriceOrStock() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.of(activeCategory(OTHER_CATEGORY_ID)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = updateProduct.execute(new UpdateProductCommand(
                PRODUCT_ID,
                OTHER_CATEGORY_ID,
                "770999",
                "Leche deslactosada",
                "Alquería",
                "900ml",
                null));

        assertEquals(OTHER_CATEGORY_ID, result.categoryId());
        assertEquals("Leche deslactosada", result.name());
        assertEquals(PRICE, result.price());
        assertEquals(10, result.stock());
        assertEquals(ProductStatus.ACTIVE, result.status());
        assertEquals(existing.createdAt(), result.createdAt());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldRejectUpdateWhenCategoryIsMissing() {
        when(productRepository.findById(PRODUCT_ID))
                .thenReturn(Optional.of(product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE)));
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(
                InvalidCategoryReferenceException.class,
                () -> updateProduct.execute(new UpdateProductCommand(
                        PRODUCT_ID, OTHER_CATEGORY_ID, "770999", "Leche", "Alpina", null, null)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldActivateProduct() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.INACTIVE, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = activateProduct.execute(new ActivateProductCommand(PRODUCT_ID));

        assertEquals(ProductStatus.ACTIVE, result.status());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldDeactivateProduct() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = deactivateProduct.execute(new DeactivateProductCommand(PRODUCT_ID));

        assertEquals(ProductStatus.INACTIVE, result.status());
        assertEquals(10, result.stock());
        assertEquals(PRICE, result.price());
    }

    @Test
    void shouldChangeProductPrice() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE);
        Money newPrice = Money.cop(new BigDecimal("5200.00"));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result =
                changeProductPrice.execute(new ChangeProductPriceCommand(PRODUCT_ID, newPrice));

        assertEquals(newPrice, result.price());
        assertEquals(10, result.stock());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldListProductsByCategoryAndStatusForAdmin() {
        Product matching = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.INACTIVE, 10, PRICE);
        when(productRepository.findByCategoryIdAndStatus(CATEGORY_ID, ProductStatus.INACTIVE))
                .thenReturn(List.of(matching));

        List<ProductResult> result = listProducts.execute(
                new ListProductsCommand(CATEGORY_ID, ProductStatus.INACTIVE, CatalogView.ADMIN));

        assertEquals(1, result.size());
        assertEquals(PRODUCT_ID, result.get(0).id());
        assertEquals(ProductStatus.INACTIVE, result.get(0).status());
    }

    @Test
    void shouldSearchProductsForAdmin() {
        Product matching = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.INACTIVE, 10, PRICE);
        when(productRepository.searchByNameBrandOrBarcode("leche")).thenReturn(List.of(matching));

        List<ProductResult> result =
                searchProducts.execute(new SearchProductsCommand("leche", CatalogView.ADMIN));

        assertEquals(1, result.size());
        assertEquals(PRODUCT_ID, result.get(0).id());
    }

    private CreateProductCommand createCommand() {
        return new CreateProductCommand(
                CATEGORY_ID, "7701234567890", "Leche entera", "Alpina", "1L", PRICE, 10, null);
    }

    private UpdateProductCommand updateCommand() {
        return new UpdateProductCommand(
                PRODUCT_ID, CATEGORY_ID, "7701234567890", "Leche entera", "Alpina", "1L", null);
    }

    private static Category activeCategory() {
        return activeCategory(CATEGORY_ID);
    }

    private static Category activeCategory(UUID id) {
        return Category.create(id, "Lácteos", null, CategoryStatus.ACTIVE, CREATED_AT, CREATED_AT);
    }

    private static Product product(
            UUID id, UUID categoryId, ProductStatus status, int stock, Money price) {
        return Product.create(
                id,
                categoryId,
                "7701234567890",
                "Leche entera",
                "Alpina",
                "1L",
                price,
                stock,
                null,
                status,
                CREATED_AT,
                CREATED_AT);
    }
}
