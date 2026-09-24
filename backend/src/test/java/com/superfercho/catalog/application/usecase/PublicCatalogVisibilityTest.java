package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private static final UUID TYPE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID INACTIVE_TYPE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID VARIANT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final Presentation UNIT = Presentation.of(1, PresentationUnit.UNIT);
    private static final UUID ACTIVE_PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INACTIVE_PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ARCHIVED_PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PRODUCT_IN_INACTIVE_CATEGORY_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PRODUCT_WITH_INACTIVE_TYPE_ID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PRODUCT_WITH_INACTIVE_VARIANT_ID =
            UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Money PRICE = Money.cop(new BigDecimal("1000.00"));

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductTypeRepository productTypeRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    private GetProductUseCase getProduct;
    private ListProductsUseCase listProducts;
    private SearchProductsUseCase searchProducts;

    @BeforeEach
    void setUp() {
        getProduct = new GetProductUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
        listProducts = new ListProductsUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
        searchProducts = new SearchProductsUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
    }

    @Test
    void shouldExcludeInactiveProductFromPublicGet() {
        when(productRepository.findById(INACTIVE_PRODUCT_ID))
                .thenReturn(Optional.of(product(INACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, TYPE_ID, null, ProductStatus.INACTIVE)));
        stubActiveTaxonomyForGet(ACTIVE_CATEGORY_ID, TYPE_ID);

        assertThrows(
                ProductNotFoundException.class,
                () -> getProduct.execute(new GetProductCommand(INACTIVE_PRODUCT_ID, CatalogView.PUBLIC)));
    }

    @Test
    void shouldExcludeArchivedProductFromPublicGet() {
        when(productRepository.findById(ARCHIVED_PRODUCT_ID))
                .thenReturn(Optional.of(product(ARCHIVED_PRODUCT_ID, ACTIVE_CATEGORY_ID, TYPE_ID, null, ProductStatus.ARCHIVED)));
        stubActiveTaxonomyForGet(ACTIVE_CATEGORY_ID, TYPE_ID);

        assertThrows(
                ProductNotFoundException.class,
                () -> getProduct.execute(new GetProductCommand(ARCHIVED_PRODUCT_ID, CatalogView.PUBLIC)));
    }

    @Test
    void shouldExcludeProductWithInactiveCategoryFromPublicGet() {
        when(productRepository.findById(PRODUCT_IN_INACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(product(
                        PRODUCT_IN_INACTIVE_CATEGORY_ID, INACTIVE_CATEGORY_ID, TYPE_ID, null, ProductStatus.ACTIVE)));
        when(categoryRepository.findById(INACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(category(INACTIVE_CATEGORY_ID, CategoryStatus.INACTIVE)));
        when(productTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(productType(TYPE_ID, ACTIVE_CATEGORY_ID, ProductTypeStatus.ACTIVE)));

        assertThrows(
                ProductNotFoundException.class,
                () -> getProduct.execute(
                        new GetProductCommand(PRODUCT_IN_INACTIVE_CATEGORY_ID, CatalogView.PUBLIC)));
    }

    @Test
    void shouldExcludeProductWithInactiveTypeFromPublicGet() {
        when(productRepository.findById(PRODUCT_WITH_INACTIVE_TYPE_ID))
                .thenReturn(Optional.of(product(
                        PRODUCT_WITH_INACTIVE_TYPE_ID,
                        ACTIVE_CATEGORY_ID,
                        INACTIVE_TYPE_ID,
                        null,
                        ProductStatus.ACTIVE)));
        when(categoryRepository.findById(ACTIVE_CATEGORY_ID))
                .thenReturn(Optional.of(category(ACTIVE_CATEGORY_ID, CategoryStatus.ACTIVE)));
        when(productTypeRepository.findById(INACTIVE_TYPE_ID))
                .thenReturn(Optional.of(productType(INACTIVE_TYPE_ID, ACTIVE_CATEGORY_ID, ProductTypeStatus.INACTIVE)));

        assertThrows(
                ProductNotFoundException.class,
                () -> getProduct.execute(new GetProductCommand(PRODUCT_WITH_INACTIVE_TYPE_ID, CatalogView.PUBLIC)));
    }

    @Test
    void shouldExcludeProductWithInactiveVariantFromPublicGet() {
        when(productRepository.findById(PRODUCT_WITH_INACTIVE_VARIANT_ID))
                .thenReturn(Optional.of(product(
                        PRODUCT_WITH_INACTIVE_VARIANT_ID,
                        ACTIVE_CATEGORY_ID,
                        TYPE_ID,
                        VARIANT_ID,
                        ProductStatus.ACTIVE)));
        stubActiveTaxonomyForGet(ACTIVE_CATEGORY_ID, TYPE_ID);
        when(productVariantRepository.findById(VARIANT_ID))
                .thenReturn(Optional.of(productVariant(VARIANT_ID, TYPE_ID, ProductVariantStatus.INACTIVE)));

        assertThrows(
                ProductNotFoundException.class,
                () -> getProduct.execute(
                        new GetProductCommand(PRODUCT_WITH_INACTIVE_VARIANT_ID, CatalogView.PUBLIC)));
    }

    @Test
    void shouldExcludeProductWhenLinkedVariantIsMissingFromPublicGet() {
        when(productRepository.findById(PRODUCT_WITH_INACTIVE_VARIANT_ID))
                .thenReturn(Optional.of(product(
                        PRODUCT_WITH_INACTIVE_VARIANT_ID,
                        ACTIVE_CATEGORY_ID,
                        TYPE_ID,
                        VARIANT_ID,
                        ProductStatus.ACTIVE)));
        stubActiveTaxonomyForGet(ACTIVE_CATEGORY_ID, TYPE_ID);
        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> getProduct.execute(
                        new GetProductCommand(PRODUCT_WITH_INACTIVE_VARIANT_ID, CatalogView.PUBLIC)));
    }

    @Test
    void shouldReturnActiveProductWithActiveTaxonomyForPublicGet() {
        when(productRepository.findById(ACTIVE_PRODUCT_ID))
                .thenReturn(Optional.of(product(ACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, TYPE_ID, null, ProductStatus.ACTIVE)));
        stubActiveTaxonomyForGet(ACTIVE_CATEGORY_ID, TYPE_ID);

        ProductResult result =
                getProduct.execute(new GetProductCommand(ACTIVE_PRODUCT_ID, CatalogView.PUBLIC));

        assertEquals(ACTIVE_PRODUCT_ID, result.id());
        verify(productVariantRepository, never()).findById(any());
    }

    @Test
    void shouldExcludeNonPublicProductsFromPublicListUsingBatchLookup() {
        Product visible = product(ACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, TYPE_ID, null, ProductStatus.ACTIVE);
        Product inactiveType =
                product(PRODUCT_WITH_INACTIVE_TYPE_ID, ACTIVE_CATEGORY_ID, INACTIVE_TYPE_ID, null, ProductStatus.ACTIVE);
        Product inactiveVariant = product(
                PRODUCT_WITH_INACTIVE_VARIANT_ID, ACTIVE_CATEGORY_ID, TYPE_ID, VARIANT_ID, ProductStatus.ACTIVE);
        when(productRepository.findByStatus(ProductStatus.ACTIVE))
                .thenReturn(List.of(visible, inactiveType, inactiveVariant));
        when(categoryRepository.findByIds(Set.of(ACTIVE_CATEGORY_ID)))
                .thenReturn(List.of(category(ACTIVE_CATEGORY_ID, CategoryStatus.ACTIVE)));
        when(productTypeRepository.findByIds(Set.of(TYPE_ID, INACTIVE_TYPE_ID)))
                .thenReturn(List.of(
                        productType(TYPE_ID, ACTIVE_CATEGORY_ID, ProductTypeStatus.ACTIVE),
                        productType(INACTIVE_TYPE_ID, ACTIVE_CATEGORY_ID, ProductTypeStatus.INACTIVE)));
        when(productVariantRepository.findByIds(Set.of(VARIANT_ID)))
                .thenReturn(List.of(productVariant(VARIANT_ID, TYPE_ID, ProductVariantStatus.INACTIVE)));

        List<ProductResult> result =
                listProducts.execute(new ListProductsCommand(null, null, CatalogView.PUBLIC));

        assertEquals(1, result.size());
        assertEquals(ACTIVE_PRODUCT_ID, result.get(0).id());
        verify(categoryRepository, times(1)).findByIds(Set.of(ACTIVE_CATEGORY_ID));
        verify(productTypeRepository, times(1)).findByIds(Set.of(TYPE_ID, INACTIVE_TYPE_ID));
        verify(productVariantRepository, times(1)).findByIds(Set.of(VARIANT_ID));
        verify(categoryRepository, never()).findById(any());
        verify(productTypeRepository, never()).findById(any());
        verify(productVariantRepository, never()).findById(any());
    }

    @Test
    void shouldExcludeNonPublicProductsFromPublicSearchUsingBatchLookup() {
        Product visible = product(ACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, TYPE_ID, null, ProductStatus.ACTIVE);
        Product inactiveType =
                product(PRODUCT_WITH_INACTIVE_TYPE_ID, ACTIVE_CATEGORY_ID, INACTIVE_TYPE_ID, null, ProductStatus.ACTIVE);
        when(productRepository.searchByNameBrandOrBarcode("leche")).thenReturn(List.of(visible, inactiveType));
        when(categoryRepository.findByIds(Set.of(ACTIVE_CATEGORY_ID)))
                .thenReturn(List.of(category(ACTIVE_CATEGORY_ID, CategoryStatus.ACTIVE)));
        when(productTypeRepository.findByIds(Set.of(TYPE_ID, INACTIVE_TYPE_ID)))
                .thenReturn(List.of(
                        productType(TYPE_ID, ACTIVE_CATEGORY_ID, ProductTypeStatus.ACTIVE),
                        productType(INACTIVE_TYPE_ID, ACTIVE_CATEGORY_ID, ProductTypeStatus.INACTIVE)));

        List<ProductResult> result =
                searchProducts.execute(new SearchProductsCommand("leche", CatalogView.PUBLIC));

        assertEquals(1, result.size());
        assertEquals(ACTIVE_PRODUCT_ID, result.get(0).id());
        verify(categoryRepository, times(1)).findByIds(Set.of(ACTIVE_CATEGORY_ID));
        verify(productTypeRepository, times(1)).findByIds(Set.of(TYPE_ID, INACTIVE_TYPE_ID));
        verify(productVariantRepository, never()).findByIds(any());
    }

    @Test
    void shouldAllowAdminToGetProductWithInactiveType() {
        when(productRepository.findById(PRODUCT_WITH_INACTIVE_TYPE_ID))
                .thenReturn(Optional.of(product(
                        PRODUCT_WITH_INACTIVE_TYPE_ID,
                        ACTIVE_CATEGORY_ID,
                        INACTIVE_TYPE_ID,
                        null,
                        ProductStatus.ACTIVE)));

        ProductResult result =
                getProduct.execute(new GetProductCommand(PRODUCT_WITH_INACTIVE_TYPE_ID, CatalogView.ADMIN));

        assertEquals(PRODUCT_WITH_INACTIVE_TYPE_ID, result.id());
        verify(categoryRepository, never()).findById(any());
        verify(productTypeRepository, never()).findById(any());
    }

    @Test
    void shouldAllowAdminToGetInactiveProduct() {
        when(productRepository.findById(INACTIVE_PRODUCT_ID))
                .thenReturn(Optional.of(product(INACTIVE_PRODUCT_ID, ACTIVE_CATEGORY_ID, TYPE_ID, null, ProductStatus.INACTIVE)));

        ProductResult result =
                getProduct.execute(new GetProductCommand(INACTIVE_PRODUCT_ID, CatalogView.ADMIN));

        assertEquals(ProductStatus.INACTIVE, result.status());
        assertTrue(result.id().equals(INACTIVE_PRODUCT_ID));
    }

    @Test
    void shouldAllowAdminToGetArchivedProduct() {
        when(productRepository.findById(ARCHIVED_PRODUCT_ID))
                .thenReturn(Optional.of(product(ARCHIVED_PRODUCT_ID, ACTIVE_CATEGORY_ID, TYPE_ID, null, ProductStatus.ARCHIVED)));

        ProductResult result =
                getProduct.execute(new GetProductCommand(ARCHIVED_PRODUCT_ID, CatalogView.ADMIN));

        assertEquals(ProductStatus.ARCHIVED, result.status());
        assertEquals(ARCHIVED_PRODUCT_ID, result.id());
    }

    private void stubActiveTaxonomyForGet(UUID categoryId, UUID typeId) {
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category(categoryId, CategoryStatus.ACTIVE)));
        when(productTypeRepository.findById(typeId))
                .thenReturn(Optional.of(productType(typeId, categoryId, ProductTypeStatus.ACTIVE)));
    }

    private static Category category(UUID id, CategoryStatus status) {
        return Category.create(id, "Lácteos", null, status, CREATED_AT, CREATED_AT);
    }

    private static ProductType productType(UUID id, UUID categoryId, ProductTypeStatus status) {
        return ProductType.create(id, categoryId, "Leche", null, status, CREATED_AT, CREATED_AT);
    }

    private static ProductVariant productVariant(UUID id, UUID typeId, ProductVariantStatus status) {
        return ProductVariant.create(id, typeId, "Entera", null, status, CREATED_AT, CREATED_AT);
    }

    private static Product product(
            UUID id, UUID categoryId, UUID typeId, UUID variantId, ProductStatus status) {
        return Product.create(
                id,
                categoryId,
                typeId,
                variantId,
                UNIT,
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
