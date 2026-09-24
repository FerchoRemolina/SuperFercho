package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ProductCardInfo;
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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindProductCardsUseCaseTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
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

    private FindProductCardsUseCase findProductCards;

    @BeforeEach
    void setUp() {
        findProductCards = new FindProductCardsUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
    }

    @Test
    void shouldReturnSellableCardWhenProductCategoryAndTypeAreActive() {
        when(productRepository.findByIds(List.of(PRODUCT_ID)))
                .thenReturn(List.of(product(PRODUCT_ID, ProductStatus.ACTIVE, null, 10)));
        stubActiveBatchTaxonomy(Set.of(CATEGORY_ID), Set.of(TYPE_ID), Set.of());

        List<ProductCardInfo> cards = findProductCards.findCardsByIds(List.of(PRODUCT_ID));

        assertEquals(1, cards.size());
        ProductCardInfo card = cards.get(0);
        assertEquals(PRODUCT_ID, card.id());
        assertEquals("Leche entera", card.name());
        assertEquals("Alpina", card.brand());
        assertEquals(PRICE, card.price());
        assertEquals(CATEGORY_ID, card.categoryId());
        assertEquals(ProductStatus.ACTIVE, card.status());
        assertTrue(card.sellable());
        verify(productRepository, times(1)).findByIds(List.of(PRODUCT_ID));
        verify(productRepository, never()).findById(any());
        verify(categoryRepository, times(1)).findByIds(Set.of(CATEGORY_ID));
        verify(productTypeRepository, times(1)).findByIds(Set.of(TYPE_ID));
        verify(productVariantRepository, never()).findByIds(any());
        verify(productTypeRepository, never()).findById(any());
    }

    @Test
    void shouldReturnInactiveProductAsNotSellable() {
        when(productRepository.findByIds(List.of(PRODUCT_ID)))
                .thenReturn(List.of(product(PRODUCT_ID, ProductStatus.INACTIVE, null, 10)));
        stubActiveBatchTaxonomy(Set.of(CATEGORY_ID), Set.of(TYPE_ID), Set.of());

        List<ProductCardInfo> cards = findProductCards.findCardsByIds(List.of(PRODUCT_ID));

        assertEquals(1, cards.size());
        assertEquals(ProductStatus.INACTIVE, cards.get(0).status());
        assertFalse(cards.get(0).sellable());
    }

    @Test
    void shouldReturnActiveProductAsNotSellableWhenCategoryIsInactive() {
        when(productRepository.findByIds(List.of(PRODUCT_ID)))
                .thenReturn(List.of(product(PRODUCT_ID, ProductStatus.ACTIVE, null, 10)));
        when(categoryRepository.findByIds(Set.of(CATEGORY_ID))).thenReturn(List.of(category(CategoryStatus.INACTIVE)));
        when(productTypeRepository.findByIds(Set.of(TYPE_ID)))
                .thenReturn(List.of(productType(ProductTypeStatus.ACTIVE)));

        List<ProductCardInfo> cards = findProductCards.findCardsByIds(List.of(PRODUCT_ID));

        assertEquals(ProductStatus.ACTIVE, cards.get(0).status());
        assertFalse(cards.get(0).sellable());
    }

    @Test
    void shouldReturnNotSellableWhenProductTypeIsInactive() {
        when(productRepository.findByIds(List.of(PRODUCT_ID)))
                .thenReturn(List.of(product(PRODUCT_ID, ProductStatus.ACTIVE, null, 10)));
        when(categoryRepository.findByIds(Set.of(CATEGORY_ID))).thenReturn(List.of(category(CategoryStatus.ACTIVE)));
        when(productTypeRepository.findByIds(Set.of(TYPE_ID)))
                .thenReturn(List.of(productType(ProductTypeStatus.INACTIVE)));

        assertFalse(findProductCards.findCardsByIds(List.of(PRODUCT_ID)).get(0).sellable());
    }

    @Test
    void shouldReturnNotSellableWhenProductVariantIsInactive() {
        when(productRepository.findByIds(List.of(PRODUCT_ID)))
                .thenReturn(List.of(product(PRODUCT_ID, ProductStatus.ACTIVE, VARIANT_ID, 10)));
        stubActiveBatchTaxonomy(Set.of(CATEGORY_ID), Set.of(TYPE_ID), Set.of(VARIANT_ID));
        when(productVariantRepository.findByIds(Set.of(VARIANT_ID)))
                .thenReturn(List.of(productVariant(ProductVariantStatus.INACTIVE)));

        assertFalse(findProductCards.findCardsByIds(List.of(PRODUCT_ID)).get(0).sellable());
    }

    @Test
    void shouldReturnSellableCardWhenStockIsZero() {
        when(productRepository.findByIds(List.of(PRODUCT_ID)))
                .thenReturn(List.of(product(PRODUCT_ID, ProductStatus.ACTIVE, null, 0)));
        stubActiveBatchTaxonomy(Set.of(CATEGORY_ID), Set.of(TYPE_ID), Set.of());

        List<ProductCardInfo> cards = findProductCards.findCardsByIds(List.of(PRODUCT_ID));

        assertTrue(cards.get(0).sellable());
    }

    @Test
    void shouldOmitMissingProductsAndLookupTaxonomyInBatch() {
        when(productRepository.findByIds(List.of(PRODUCT_ID, SECOND_PRODUCT_ID)))
                .thenReturn(List.of(product(PRODUCT_ID, ProductStatus.ACTIVE, null, 10)));
        stubActiveBatchTaxonomy(Set.of(CATEGORY_ID), Set.of(TYPE_ID), Set.of());

        List<ProductCardInfo> cards = findProductCards.findCardsByIds(List.of(PRODUCT_ID, SECOND_PRODUCT_ID));

        assertEquals(1, cards.size());
        assertEquals(PRODUCT_ID, cards.get(0).id());
        verify(productRepository, times(1)).findByIds(List.of(PRODUCT_ID, SECOND_PRODUCT_ID));
        verify(categoryRepository, times(1)).findByIds(Set.of(CATEGORY_ID));
        verify(productTypeRepository, times(1)).findByIds(Set.of(TYPE_ID));
    }

    @Test
    void shouldReturnEmptyWithoutQueryingWhenIdsAreEmpty() {
        assertTrue(findProductCards.findCardsByIds(List.of()).isEmpty());

        verify(productRepository, never()).findByIds(any());
        verify(categoryRepository, never()).findByIds(any());
        verify(productTypeRepository, never()).findByIds(any());
        verify(productVariantRepository, never()).findByIds(any());
    }

    private void stubActiveBatchTaxonomy(Set<UUID> categoryIds, Set<UUID> typeIds, Set<UUID> variantIds) {
        when(categoryRepository.findByIds(categoryIds)).thenReturn(List.of(category(CategoryStatus.ACTIVE)));
        when(productTypeRepository.findByIds(typeIds)).thenReturn(List.of(productType(ProductTypeStatus.ACTIVE)));
        if (!variantIds.isEmpty()) {
            when(productVariantRepository.findByIds(variantIds))
                    .thenReturn(List.of(productVariant(ProductVariantStatus.ACTIVE)));
        }
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

    private static Product product(UUID id, ProductStatus status, UUID variantId, int stock) {
        return Product.create(
                id,
                CATEGORY_ID,
                TYPE_ID,
                variantId,
                UNIT,
                "7701234567890",
                "Leche entera",
                "Alpina",
                "1L",
                PRICE,
                stock,
                "https://img.test/milk.png",
                status,
                CREATED_AT,
                CREATED_AT);
    }
}
