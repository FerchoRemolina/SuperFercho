package com.superfercho.catalog.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.infrastructure.persistence.entity.CategoryJpaEntity;
import com.superfercho.catalog.infrastructure.persistence.entity.ProductJpaEntity;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoryAndProductPersistenceMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TYPE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID PRODUCT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Presentation UNIT = Presentation.of(1, PresentationUnit.UNIT);

    private final CategoryPersistenceMapper categoryMapper = new CategoryPersistenceMapper();
    private final ProductPersistenceMapper productMapper = new ProductPersistenceMapper();

    @Test
    void shouldMapCategoryRoundTrip() {
        Category category = Category.create(
                CATEGORY_ID, "Frutas", "Fresh produce", CategoryStatus.ACTIVE, NOW, NOW);

        CategoryJpaEntity entity = categoryMapper.toEntity(category);
        Category mapped = categoryMapper.toDomain(entity);

        assertEquals(category.id(), mapped.id());
        assertEquals(category.name(), mapped.name());
        assertEquals(category.description(), mapped.description());
        assertEquals(category.status(), mapped.status());
        assertEquals(category.createdAt(), mapped.createdAt());
        assertEquals(category.updatedAt(), mapped.updatedAt());
    }

    @Test
    void shouldMapProductRoundTripIncludingOptionalFieldsAndMoney() {
        Product product = Product.create(
                PRODUCT_ID,
                CATEGORY_ID,
                TYPE_ID,
                null,
                UNIT,
                "7701234567890",
                "Leche entera",
                "Alpina",
                "1L",
                Money.cop(new BigDecimal("4500.50")),
                10,
                "https://cdn.example.com/leche.png",
                ProductStatus.ACTIVE,
                NOW,
                NOW);

        ProductJpaEntity entity = productMapper.toEntity(product);
        Product mapped = productMapper.toDomain(entity);

        assertEquals(product.id(), mapped.id());
        assertEquals(product.categoryId(), mapped.categoryId());
        assertEquals(product.productTypeId(), mapped.productTypeId());
        assertNull(mapped.productVariantId());
        assertEquals(product.presentation(), mapped.presentation());
        assertEquals(product.barcode(), mapped.barcode());
        assertEquals(product.name(), mapped.name());
        assertEquals(product.brand(), mapped.brand());
        assertEquals(product.description(), mapped.description());
        assertEquals(product.price(), mapped.price());
        assertEquals(new BigDecimal("4500.50"), entity.getPriceAmount());
        assertEquals(Money.COP, entity.getCurrency());
        assertEquals(product.stock(), mapped.stock());
        assertEquals(product.imageUrl(), mapped.imageUrl());
        assertEquals(product.status(), mapped.status());
    }

    @Test
    void shouldMapProductWhenOptionalFieldsAreNull() {
        Product product = Product.create(
                PRODUCT_ID,
                CATEGORY_ID,
                TYPE_ID,
                null,
                UNIT,
                null,
                "Leche entera",
                null,
                null,
                Money.cop(new BigDecimal("1000.00")),
                0,
                null,
                ProductStatus.INACTIVE,
                NOW,
                NOW);

        Product mapped = productMapper.toDomain(productMapper.toEntity(product));

        assertNull(mapped.barcode());
        assertNull(mapped.brand());
        assertNull(mapped.description());
        assertNull(mapped.imageUrl());
        assertNull(mapped.productVariantId());
        assertEquals(ProductStatus.INACTIVE, mapped.status());
        assertEquals(0, mapped.stock());
    }
}
