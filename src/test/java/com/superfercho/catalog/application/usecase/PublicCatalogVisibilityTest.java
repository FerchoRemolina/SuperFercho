package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
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
class PublicCatalogVisibilityTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID ACTIVE_CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID INACTIVE_CATEGORY_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ACTIVE_PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INACTIVE_PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_IN_INACTIVE_CATEGORY_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Money PRICE = Money.cop(new BigDecimal("1000.00"));

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private GetProductUseCase getProduct;
    private ListProductsUseCase listProducts;
    private SearchProductsUseCase searchProducts;

    @BeforeEach
    void setUp() {
        getProduct = new GetProductUseCase(productRepository, categoryRepository);
        listProducts = new ListProductsUseCase(productRepository, categoryRepository);
        searchProducts = new SearchProductsUseCase(productRepository, categoryRepository);
    }

    @Test
    void shouldExcludeInactiveProductFromPublicGet() {
        when(productRepository.findById(INACTIVE_PRODUCT_ID))
                .thenReturn(Optional.of(product(INACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, ProductStatus.INACTIVE)));
        when(categoryRepository.findById(ACTIVE_CATEGORY_ID)).thenReturn(Optional.of(category(ACTIVE_CATEGORY_ID, CategoryStatus.ACTIVE)));

        assertThrows(
                ProductNotFoundException.class,
                () -> getProduct.execute(new GetProductCommand(INACTIVE_PRODUCT_ID, CatalogView.PUBLIC)));
    }

    @Test
    void shouldExcludeProductWithInactiveCategoryFromPublicGet() {
        when(productRepository.findById(PRODUCT_IN_INACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(
                        product(PRODUCT_IN_INACTIVE_CATEGORY_ID, INACTIVE_CATEGORY_ID, ProductStatus.ACTIVE)));
        when(categoryRepository.findById(INACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(category(INACTIVE_CATEGORY_ID, CategoryStatus.INACTIVE)));

        assertThrows(
                ProductNotFoundException.class,
                () -> getProduct.execute(
                        new GetProductCommand(PRODUCT_IN_INACTIVE_CATEGORY_ID, CatalogView.PUBLIC)));
    }

    @Test
    void shouldReturnActiveProductWithActiveCategoryForPublicGet() {
        when(productRepository.findById(ACTIVE_PRODUCT_ID))
                .thenReturn(Optional.of(product(ACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, ProductStatus.ACTIVE)));
        when(categoryRepository.findById(ACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(category(ACTIVE_CATEGORY_ID, CategoryStatus.ACTIVE)));

        ProductResult result =
                getProduct.execute(new GetProductCommand(ACTIVE_PRODUCT_ID, CatalogView.PUBLIC));

        assertEquals(ACTIVE_PRODUCT_ID, result.id());
    }

    @Test
    void shouldExcludeInactiveProductsAndProductsWithInactiveCategoryFromPublicList() {
        Product visible = product(ACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, ProductStatus.ACTIVE);
        Product inactiveProduct = product(INACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, ProductStatus.INACTIVE);
        Product activeInInactiveCategory =
                product(PRODUCT_IN_INACTIVE_CATEGORY_ID, INACTIVE_CATEGORY_ID, ProductStatus.ACTIVE);
        when(productRepository.findByStatus(ProductStatus.ACTIVE))
                .thenReturn(List.of(visible, inactiveProduct, activeInInactiveCategory));
        when(categoryRepository.findById(ACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(category(ACTIVE_CATEGORY_ID, CategoryStatus.ACTIVE)));
        when(categoryRepository.findById(INACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(category(INACTIVE_CATEGORY_ID, CategoryStatus.INACTIVE)));

        List<ProductResult> result =
                listProducts.execute(new ListProductsCommand(null, null, CatalogView.PUBLIC));

        assertEquals(1, result.size());
        assertEquals(ACTIVE_PRODUCT_ID, result.get(0).id());
    }

    @Test
    void shouldExcludeNonPublicProductsFromPublicSearch() {
        Product visible = product(ACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, ProductStatus.ACTIVE);
        Product inactiveProduct = product(INACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, ProductStatus.INACTIVE);
        when(productRepository.searchByNameBrandOrBarcode("leche"))
                .thenReturn(List.of(visible, inactiveProduct));
        when(categoryRepository.findById(ACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(category(ACTIVE_CATEGORY_ID, CategoryStatus.ACTIVE)));

        List<ProductResult> result =
                searchProducts.execute(new SearchProductsCommand("leche", CatalogView.PUBLIC));

        assertEquals(1, result.size());
        assertEquals(ACTIVE_PRODUCT_ID, result.get(0).id());
    }

    @Test
    void shouldAllowAdminToGetInactiveProduct() {
        when(productRepository.findById(INACTIVE_PRODUCT_ID))
                .thenReturn(Optional.of(product(INACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, ProductStatus.INACTIVE)));

        ProductResult result =
                getProduct.execute(new GetProductCommand(INACTIVE_PRODUCT_ID, CatalogView.ADMIN));

        assertEquals(ProductStatus.INACTIVE, result.status());
        assertTrue(result.id().equals(INACTIVE_PRODUCT_ID));
    }

    private static Category category(UUID id, CategoryStatus status) {
        return Category.create(id, "Lácteos", null, status, CREATED_AT, CREATED_AT);
    }

    private static Product product(UUID id, UUID categoryId, ProductStatus status) {
        return Product.create(
                id,
                categoryId,
                "7701234567890",
                "Leche entera",
                "Alpina",
                "1L",
                PRICE,
                5,
                null,
                status,
                CREATED_AT,
                CREATED_AT);
    }
}
