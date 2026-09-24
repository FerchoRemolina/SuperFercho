package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductCatalogOrderingTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TYPE_A = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TYPE_B = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID VARIANT_A = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID VARIANT_B = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final Money PRICE = Money.cop(new BigDecimal("1000.00"));

    @Test
    void shouldSortByNameCaseInsensitiveThenPresentationThenId() {
        UUID idBanana = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID idAppleSmall = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID idAppleLarge = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID idAppleSameQty = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Product banana = product(idBanana, TYPE_A, null, "banana", Presentation.of(1, PresentationUnit.UNIT));
        Product apple900 = product(idAppleSmall, TYPE_A, null, "Apple", Presentation.of(900, PresentationUnit.ML));
        Product apple1L = product(idAppleLarge, TYPE_A, null, "apple", Presentation.of(1, PresentationUnit.L));
        Product apple1LEarlierId =
                product(idAppleSameQty, TYPE_A, null, "APPLE", Presentation.of(1, PresentationUnit.L));

        Map<UUID, ProductType> types = Map.of(TYPE_A, type(TYPE_A, "General"));
        List<Product> sorted =
                ProductCatalogOrdering.sorted(List.of(banana, apple900, apple1L, apple1LEarlierId), types, Map.of());

        assertEquals(List.of(idAppleSameQty, idAppleLarge, idAppleSmall, idBanana), sorted.stream().map(Product::id).toList());
    }

    @Test
    void shouldUseTypeAndVariantNamesOnlyAsTieBreakers() {
        Product milkTypeA = product(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                TYPE_A,
                VARIANT_B,
                "Leche",
                Presentation.of(1, PresentationUnit.L));
        Product milkTypeB = product(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                TYPE_B,
                VARIANT_A,
                "Leche",
                Presentation.of(1, PresentationUnit.L));

        Map<UUID, ProductType> types = Map.of(
                TYPE_A, type(TYPE_A, "Entera"),
                TYPE_B, type(TYPE_B, "Deslactosada"));
        Map<UUID, ProductVariant> variants = Map.of(
                VARIANT_A, variant(VARIANT_A, TYPE_B, "A-Variant"),
                VARIANT_B, variant(VARIANT_B, TYPE_A, "Z-Variant"));

        List<Product> sorted =
                ProductCatalogOrdering.sorted(List.of(milkTypeA, milkTypeB), types, variants);

        assertEquals(TYPE_B, sorted.get(0).productTypeId());
        assertEquals(TYPE_A, sorted.get(1).productTypeId());
    }

    @Test
    void shouldOrderMassAndVolumeWithNormalizedQuantities() {
        Product twoKg = product(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                TYPE_A,
                null,
                "Harina",
                Presentation.of(2, PresentationUnit.KG));
        Product fiveHundredG = product(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                TYPE_A,
                null,
                "Harina",
                Presentation.of(500, PresentationUnit.G));
        Product oneL = product(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                TYPE_A,
                null,
                "Leche",
                Presentation.of(1, PresentationUnit.L));
        Product nineHundredMl = product(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                TYPE_A,
                null,
                "Leche",
                Presentation.of(900, PresentationUnit.ML));

        Map<UUID, ProductType> types = Map.of(TYPE_A, type(TYPE_A, "General"));
        List<Product> flour =
                ProductCatalogOrdering.sorted(List.of(fiveHundredG, twoKg), types, Map.of());
        List<Product> milk =
                ProductCatalogOrdering.sorted(List.of(nineHundredMl, oneL), types, Map.of());

        assertEquals(twoKg.id(), flour.get(0).id());
        assertEquals(fiveHundredG.id(), flour.get(1).id());
        assertEquals(oneL.id(), milk.get(0).id());
        assertEquals(nineHundredMl.id(), milk.get(1).id());
    }

    private static Product product(
            UUID id, UUID typeId, UUID variantId, String name, Presentation presentation) {
        return Product.create(
                id,
                CATEGORY_ID,
                typeId,
                variantId,
                presentation,
                null,
                name,
                null,
                null,
                PRICE,
                1,
                null,
                ProductStatus.ACTIVE,
                NOW,
                NOW);
    }

    private static ProductType type(UUID id, String name) {
        return ProductType.create(id, CATEGORY_ID, name, null, ProductTypeStatus.ACTIVE, NOW, NOW);
    }

    private static ProductVariant variant(UUID id, UUID typeId, String name) {
        return ProductVariant.create(id, typeId, name, null, ProductVariantStatus.ACTIVE, NOW, NOW);
    }
}
