package com.superfercho.shopping.infrastructure.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ProductPriceInfo;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.usecase.FindProductPriceUseCase;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductCatalogAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CATEGORY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void shouldMapCatalogPriceToShoppingInfo() {
        ProductCatalogAdapter adapter = new ProductCatalogAdapter(
                productId -> Optional.of(new ProductPriceInfo(productId, PRICE)));

        Optional<ProductCatalogInfo> result = adapter.getProduct(PRODUCT_ID);

        assertTrue(result.isPresent());
        assertEquals(PRODUCT_ID, result.get().productId());
        assertEquals(PRICE, result.get().currentPrice());
    }

    @Test
    void shouldReturnEmptyWhenCatalogHasNoProduct() {
        ProductCatalogAdapter adapter = new ProductCatalogAdapter(productId -> Optional.empty());

        assertTrue(adapter.getProduct(PRODUCT_ID).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenCatalogProductIsInactive() {
        givenProduct(ProductStatus.INACTIVE, CategoryStatus.ACTIVE);

        assertTrue(catalogAdapter().getProduct(PRODUCT_ID).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenCategoryIsInactive() {
        givenProduct(ProductStatus.ACTIVE, CategoryStatus.INACTIVE);

        assertTrue(catalogAdapter().getProduct(PRODUCT_ID).isEmpty());
    }

    @Test
    void shouldMapActiveProductWithActiveCategoryFromCatalogQuery() {
        givenProduct(ProductStatus.ACTIVE, CategoryStatus.ACTIVE);

        Optional<ProductCatalogInfo> result = catalogAdapter().getProduct(PRODUCT_ID);

        assertTrue(result.isPresent());
        assertEquals(PRODUCT_ID, result.get().productId());
        assertEquals(PRICE, result.get().currentPrice());
    }

    private ProductCatalogAdapter catalogAdapter() {
        return new ProductCatalogAdapter(new FindProductPriceUseCase(productRepository, categoryRepository));
    }

    private void givenProduct(ProductStatus productStatus, CategoryStatus categoryStatus) {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(productStatus)));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category(categoryStatus)));
    }

    private static Category category(CategoryStatus status) {
        return Category.create(CATEGORY_ID, "Lácteos", null, status, CREATED_AT, CREATED_AT);
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
