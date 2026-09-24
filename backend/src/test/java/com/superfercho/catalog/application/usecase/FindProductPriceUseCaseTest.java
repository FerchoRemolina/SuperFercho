package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ProductPriceInfo;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindProductPriceUseCaseTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TYPE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID VARIANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Presentation UNIT = Presentation.of(1, PresentationUnit.UNIT);
    private static final Money PRICE = Money.cop(new BigDecimal("4500.00"));

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductTypeRepository productTypeRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    private FindProductPriceUseCase findProductPrice;

    @BeforeEach
    void setUp() {
        findProductPrice = new FindProductPriceUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
    }

    @Test
    void shouldReturnProductWhenProductCategoryAndTypeAreActiveWithoutVariant() {
        givenProduct(ProductStatus.ACTIVE, null);
        givenCategory(CategoryStatus.ACTIVE);
        givenType(ProductTypeStatus.ACTIVE);

        Optional<ProductPriceInfo> result = findProductPrice.findById(PRODUCT_ID);

        assertTrue(result.isPresent());
        assertEquals(PRODUCT_ID, result.get().productId());
        assertEquals(PRICE, result.get().currentPrice());
    }

    @Test
    void shouldReturnEmptyWhenProductDoesNotExist() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        Optional<ProductPriceInfo> result = findProductPrice.findById(PRODUCT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenProductIsInactiveAndCategoryIsActive() {
        givenProduct(ProductStatus.INACTIVE, null);
        givenCategory(CategoryStatus.ACTIVE);
        givenType(ProductTypeStatus.ACTIVE);

        assertTrue(findProductPrice.findById(PRODUCT_ID).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenProductIsActiveAndCategoryIsInactive() {
        givenProduct(ProductStatus.ACTIVE, null);
        givenCategory(CategoryStatus.INACTIVE);
        givenType(ProductTypeStatus.ACTIVE);

        assertTrue(findProductPrice.findById(PRODUCT_ID).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenProductTypeIsInactive() {
        givenProduct(ProductStatus.ACTIVE, null);
        givenCategory(CategoryStatus.ACTIVE);
        givenType(ProductTypeStatus.INACTIVE);

        assertTrue(findProductPrice.findById(PRODUCT_ID).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenProductVariantIsInactive() {
        givenProduct(ProductStatus.ACTIVE, VARIANT_ID);
        givenCategory(CategoryStatus.ACTIVE);
        givenType(ProductTypeStatus.ACTIVE);
        when(productVariantRepository.findById(VARIANT_ID))
                .thenReturn(Optional.of(productVariant(ProductVariantStatus.INACTIVE)));

        assertTrue(findProductPrice.findById(PRODUCT_ID).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenLinkedVariantIsMissing() {
        givenProduct(ProductStatus.ACTIVE, VARIANT_ID);
        givenCategory(CategoryStatus.ACTIVE);
        givenType(ProductTypeStatus.ACTIVE);
        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.empty());

        assertTrue(findProductPrice.findById(PRODUCT_ID).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenProductAndCategoryAreInactive() {
        givenProduct(ProductStatus.INACTIVE, null);
        givenCategory(CategoryStatus.INACTIVE);
        givenType(ProductTypeStatus.ACTIVE);

        assertTrue(findProductPrice.findById(PRODUCT_ID).isEmpty());
    }

    private void givenProduct(ProductStatus productStatus, UUID variantId) {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(productStatus, variantId)));
    }

    private void givenCategory(CategoryStatus categoryStatus) {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category(categoryStatus)));
    }

    private void givenType(ProductTypeStatus typeStatus) {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(productType(typeStatus)));
    }

    private static Category category(CategoryStatus status) {
        return Category.create(CATEGORY_ID, "Lácteos", null, status, CREATED_AT, CREATED_AT);
    }

    private static ProductType productType(ProductTypeStatus status) {
        return ProductType.create(TYPE_ID, CATEGORY_ID, "Leche", null, status, CREATED_AT, CREATED_AT);
    }

    private static ProductVariant productVariant(ProductVariantStatus status) {
        return ProductVariant.create(VARIANT_ID, TYPE_ID, "Entera", null, status, CREATED_AT, CREATED_AT);
    }

    private static Product product(ProductStatus status, UUID variantId) {
        return Product.create(
                PRODUCT_ID,
                CATEGORY_ID,
                TYPE_ID,
                variantId,
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
