package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El contrato público de producto debe exponer la taxonomía comercial real
 * (Tipo, Variante, Presentación) para que Customer construya los filtros
 * jerárquicos Categoría → Tipo → Variante → Presentación sin hardcodear
 * nombres ni resolver UUIDs.
 */
@ExtendWith(MockitoExtension.class)
class PublicProductTaxonomyTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T00:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DRINKS_TYPE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID DAIRY_TYPE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID ORIGINAL_VARIANT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Presentation SIX_HUNDRED_ML = Presentation.of(600, PresentationUnit.ML);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductTypeRepository productTypeRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    private ListProductsUseCase listProducts;
    private SearchProductsUseCase searchProducts;
    private GetProductUseCase getProduct;

    @BeforeEach
    void setUp() {
        listProducts = new ListProductsUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
        searchProducts = new SearchProductsUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
        getProduct = new GetProductUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
    }

    @Test
    void publicListResolvesCommercialNamesForEveryProduct() {
        Product soda = product("1", DRINKS_TYPE_ID, ORIGINAL_VARIANT_ID);
        Product milk = product("2", DAIRY_TYPE_ID, null);
        when(productRepository.findByCategoryIdAndStatus(CATEGORY_ID, ProductStatus.ACTIVE))
                .thenReturn(List.of(soda, milk));
        when(categoryRepository.findByIds(anyCollection()))
                .thenReturn(List.of(category()));
        when(productTypeRepository.findByIds(anyCollection()))
                .thenReturn(List.of(productType(DRINKS_TYPE_ID, "Gaseosas"), productType(DAIRY_TYPE_ID, "Lácteos")));
        when(productVariantRepository.findByIds(anyCollection()))
                .thenReturn(List.of(productVariant(ORIGINAL_VARIANT_ID, "Original")));

        List<ProductResult> results =
                listProducts.execute(new ListProductsCommand(CATEGORY_ID, null, CatalogView.PUBLIC));

        assertEquals(2, results.size());
        ProductResult sodaResult = results.get(0);
        assertEquals(DRINKS_TYPE_ID, sodaResult.productTypeId());
        assertEquals("Gaseosas", sodaResult.productTypeName());
        assertEquals(ORIGINAL_VARIANT_ID, sodaResult.productVariantId());
        assertEquals("Original", sodaResult.productVariantName());
        assertEquals(SIX_HUNDRED_ML, sodaResult.presentation());

        // Producto sin variante: nombre del tipo presente, variante nula.
        ProductResult milkResult = results.get(1);
        assertEquals("Lácteos", milkResult.productTypeName());
        assertNull(milkResult.productVariantId());
        assertNull(milkResult.productVariantName());
        // Los ids existentes se conservan en el contrato.
        assertEquals(UUID.fromString(soda.id().toString()), sodaResult.id());
    }

    @Test
    void publicSearchResolvesCommercialNamesToo() {
        Product soda = product("1", DRINKS_TYPE_ID, ORIGINAL_VARIANT_ID);
        when(productRepository.searchByNameBrandOrBarcode("gaseosa")).thenReturn(List.of(soda));
        when(categoryRepository.findByIds(anyCollection())).thenReturn(List.of(category()));
        when(productTypeRepository.findByIds(anyCollection()))
                .thenReturn(List.of(productType(DRINKS_TYPE_ID, "Gaseosas")));
        when(productVariantRepository.findByIds(anyCollection()))
                .thenReturn(List.of(productVariant(ORIGINAL_VARIANT_ID, "Original")));

        List<ProductResult> results =
                searchProducts.execute(new SearchProductsCommand("gaseosa", CatalogView.PUBLIC));

        assertEquals(1, results.size());
        assertEquals("Gaseosas", results.get(0).productTypeName());
        assertEquals("Original", results.get(0).productVariantName());
    }

    @Test
    void publicGetByIdResolvesCommercialNames() {
        Product soda = product("1", DRINKS_TYPE_ID, ORIGINAL_VARIANT_ID);
        when(productRepository.findById(soda.id())).thenReturn(Optional.of(soda));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category()));
        when(productTypeRepository.findById(DRINKS_TYPE_ID))
                .thenReturn(Optional.of(productType(DRINKS_TYPE_ID, "Gaseosas")));
        when(productVariantRepository.findById(ORIGINAL_VARIANT_ID))
                .thenReturn(Optional.of(productVariant(ORIGINAL_VARIANT_ID, "Original")));

        ProductResult result = getProduct.execute(new GetProductCommand(soda.id(), CatalogView.PUBLIC));

        assertEquals("Gaseosas", result.productTypeName());
        assertEquals("Original", result.productVariantName());
        assertEquals(SIX_HUNDRED_ML, result.presentation());
    }

    @Test
    void adminGetKeepsSkippingTaxonomyLookupsAndReturnsNullNames() {
        Product soda = product("1", DRINKS_TYPE_ID, ORIGINAL_VARIANT_ID);
        when(productRepository.findById(soda.id())).thenReturn(Optional.of(soda));

        ProductResult result = getProduct.execute(new GetProductCommand(soda.id(), CatalogView.ADMIN));

        // La vista ADMIN no resuelve taxonomía (optimización garantizada por contrato):
        // los nombres son aditivos y solo se prometen en los flujos públicos.
        assertNull(result.productTypeName());
        assertNull(result.productVariantName());
        org.mockito.Mockito.verify(productTypeRepository, org.mockito.Mockito.never())
                .findById(any());
        org.mockito.Mockito.verify(productVariantRepository, org.mockito.Mockito.never())
                .findById(any());
    }

    @Test
    void unresolvedTaxonomyDegradatesToNullNamesWithoutBreakingTheContract() {
        Product orphan = product("1", DRINKS_TYPE_ID, ORIGINAL_VARIANT_ID);
        when(productRepository.findAll()).thenReturn(List.of(orphan));
        when(categoryRepository.findByIds(anyCollection())).thenReturn(List.of(category()));
        // Tipo/variante no resueltos (p. ej. datos legacy inconsistentes).
        when(productTypeRepository.findByIds(anyCollection())).thenReturn(List.of());
        when(productVariantRepository.findByIds(anyCollection())).thenReturn(List.of());

        // En vista pública un producto sin tipo resuelto no es visible (por diseño);
        // la degradación a nombres nulos se ejerce en vista administrativa.
        List<ProductResult> results =
                listProducts.execute(new ListProductsCommand(null, null, CatalogView.ADMIN));

        assertEquals(1, results.size());
        assertNull(results.get(0).productTypeName());
        assertNull(results.get(0).productVariantName());
        assertEquals(DRINKS_TYPE_ID, results.get(0).productTypeId());
    }

    @Test
    void namesAlwaysComeFromTheProductOwnTaxonomyIds() {
        // Dos productos de tipos distintos reciben cada uno el nombre de SU tipo:
        // la jerarquía proviene de las relaciones reales, no de datos globales.
        Product soda = product("1", DRINKS_TYPE_ID, ORIGINAL_VARIANT_ID);
        Product milk = product("2", DAIRY_TYPE_ID, null);
        when(productRepository.findAll()).thenReturn(List.of(soda, milk));
        when(categoryRepository.findByIds(anyCollection())).thenReturn(List.of(category()));
        when(productTypeRepository.findByIds(anyCollection()))
                .thenReturn(List.of(productType(DRINKS_TYPE_ID, "Gaseosas"), productType(DAIRY_TYPE_ID, "Lácteos")));
        when(productVariantRepository.findByIds(anyCollection()))
                .thenReturn(List.of(productVariant(ORIGINAL_VARIANT_ID, "Original")));

        List<ProductResult> results =
                listProducts.execute(new ListProductsCommand(null, null, CatalogView.ADMIN));

        assertEquals("Gaseosas", results.get(0).productTypeName());
        assertEquals("Lácteos", results.get(1).productTypeName());
    }

    private Product product(String suffix, UUID productTypeId, UUID productVariantId) {
        return Product.create(
                UUID.nameUUIDFromBytes(suffix.getBytes()),
                CATEGORY_ID,
                productTypeId,
                productVariantId,
                SIX_HUNDRED_ML,
                null,
                "Producto " + suffix,
                null,
                null,
                Money.cop(new BigDecimal("4500.00")),
                10,
                null,
                ProductStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT);
    }

    private Category category() {
        return Category.create(
                CATEGORY_ID,
                "Bebidas",
                null,
                com.superfercho.catalog.domain.model.CategoryIcon.DRINKS,
                CategoryStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT);
    }

    private ProductType productType(UUID id, String name) {
        return ProductType.create(
                id, CATEGORY_ID, name, null, ProductTypeStatus.ACTIVE, CREATED_AT, UPDATED_AT);
    }

    private ProductVariant productVariant(UUID id, String name) {
        return ProductVariant.create(
                id, DRINKS_TYPE_ID, name, null, ProductVariantStatus.ACTIVE, CREATED_AT, UPDATED_AT);
    }
}
