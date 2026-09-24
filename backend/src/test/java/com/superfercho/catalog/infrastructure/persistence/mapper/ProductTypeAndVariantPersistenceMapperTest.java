package com.superfercho.catalog.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductTypeAndVariantPersistenceMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TYPE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VARIANT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final ProductTypePersistenceMapper typeMapper = new ProductTypePersistenceMapper();
    private final ProductVariantPersistenceMapper variantMapper = new ProductVariantPersistenceMapper();

    @Test
    void shouldMapProductTypeRoundTrip() {
        ProductType type = ProductType.create(
                TYPE_ID, CATEGORY_ID, "Manzana", "Fruta", ProductTypeStatus.ACTIVE, NOW, NOW);

        ProductType mapped = typeMapper.toDomain(typeMapper.toEntity(type));

        assertEquals(type.id(), mapped.id());
        assertEquals(type.categoryId(), mapped.categoryId());
        assertEquals(type.name(), mapped.name());
        assertEquals(type.description(), mapped.description());
        assertEquals(type.status(), mapped.status());
    }

    @Test
    void shouldMapProductVariantRoundTrip() {
        ProductVariant variant = ProductVariant.create(
                VARIANT_ID, TYPE_ID, "Roja", null, ProductVariantStatus.INACTIVE, NOW, NOW);

        ProductVariant mapped = variantMapper.toDomain(variantMapper.toEntity(variant));

        assertEquals(variant.id(), mapped.id());
        assertEquals(variant.productTypeId(), mapped.productTypeId());
        assertEquals(variant.name(), mapped.name());
        assertNull(mapped.description());
        assertEquals(ProductVariantStatus.INACTIVE, mapped.status());
    }
}
