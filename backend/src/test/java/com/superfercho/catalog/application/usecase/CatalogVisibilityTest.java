package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogVisibilityTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TYPE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VARIANT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Presentation UNIT = Presentation.of(1, PresentationUnit.UNIT);
    private static final Money PRICE = Money.cop(new BigDecimal("1000.00"));

    @Test
    void visibleWhenProductCategoryTypeAndVariantAreActive() {
        assertTrue(CatalogVisibility.isPubliclyVisible(
                product(ProductStatus.ACTIVE, VARIANT_ID),
                category(CategoryStatus.ACTIVE),
                productType(ProductTypeStatus.ACTIVE),
                productVariant(ProductVariantStatus.ACTIVE)));
    }

    @Test
    void invisibleWhenProductTypeIsInactive() {
        assertFalse(CatalogVisibility.isPubliclyVisible(
                product(ProductStatus.ACTIVE, VARIANT_ID),
                category(CategoryStatus.ACTIVE),
                productType(ProductTypeStatus.INACTIVE),
                productVariant(ProductVariantStatus.ACTIVE)));
    }

    @Test
    void invisibleWhenProductVariantIsInactive() {
        assertFalse(CatalogVisibility.isPubliclyVisible(
                product(ProductStatus.ACTIVE, VARIANT_ID),
                category(CategoryStatus.ACTIVE),
                productType(ProductTypeStatus.ACTIVE),
                productVariant(ProductVariantStatus.INACTIVE)));
    }

    @Test
    void visibleWhenVariantIsNullAndTypeIsActive() {
        assertTrue(CatalogVisibility.isPubliclyVisible(
                product(ProductStatus.ACTIVE, null),
                category(CategoryStatus.ACTIVE),
                productType(ProductTypeStatus.ACTIVE),
                null));
    }

    @Test
    void invisibleWhenProductIsInactive() {
        assertFalse(CatalogVisibility.isPubliclyVisible(
                product(ProductStatus.INACTIVE, null),
                category(CategoryStatus.ACTIVE),
                productType(ProductTypeStatus.ACTIVE),
                null));
    }

    @Test
    void invisibleWhenProductIsArchived() {
        assertFalse(CatalogVisibility.isPubliclyVisible(
                product(ProductStatus.ARCHIVED, null),
                category(CategoryStatus.ACTIVE),
                productType(ProductTypeStatus.ACTIVE),
                null));
    }

    @Test
    void invisibleWhenCategoryIsInactive() {
        assertFalse(CatalogVisibility.isPubliclyVisible(
                product(ProductStatus.ACTIVE, null),
                category(CategoryStatus.INACTIVE),
                productType(ProductTypeStatus.ACTIVE),
                null));
    }

    @Test
    void invisibleWhenProductTypeIsMissing() {
        assertFalse(CatalogVisibility.isPubliclyVisible(
                product(ProductStatus.ACTIVE, null),
                category(CategoryStatus.ACTIVE),
                null,
                null));
    }

    @Test
    void invisibleWhenLinkedVariantIsMissing() {
        assertFalse(CatalogVisibility.isPubliclyVisible(
                product(ProductStatus.ACTIVE, VARIANT_ID),
                category(CategoryStatus.ACTIVE),
                productType(ProductTypeStatus.ACTIVE),
                null));
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
        int stock = status == ProductStatus.ARCHIVED ? 0 : 5;
        return Product.create(
                PRODUCT_ID,
                CATEGORY_ID,
                TYPE_ID,
                variantId,
                UNIT,
                "7701234567890",
                "Leche",
                "Alpina",
                "1L",
                PRICE,
                stock,
                null,
                status,
                CREATED_AT,
                CREATED_AT);
    }
}
