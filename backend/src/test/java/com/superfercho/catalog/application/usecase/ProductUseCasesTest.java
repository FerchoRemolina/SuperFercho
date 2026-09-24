package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ActivateProductCommand;
import com.superfercho.catalog.application.dto.AdjustProductStockCommand;
import com.superfercho.catalog.application.dto.ArchiveProductCommand;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ChangeProductPriceCommand;
import com.superfercho.catalog.application.dto.CreateProductCommand;
import com.superfercho.catalog.application.dto.DeactivateProductCommand;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.RestoreProductCommand;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
import com.superfercho.catalog.application.dto.UpdateProductCommand;
import com.superfercho.catalog.application.exception.InvalidProductTypeReferenceException;
import com.superfercho.catalog.application.exception.InvalidProductVariantReferenceException;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.exception.ProductStockConflictException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.exception.InvalidProductException;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TYPE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OTHER_TYPE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID VARIANT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID OTHER_VARIANT_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final Presentation UNIT = Presentation.of(1, PresentationUnit.UNIT);
    private static final Presentation LITER = Presentation.of(1, PresentationUnit.L);
    private static final Money PRICE = Money.cop(new BigDecimal("4500.00"));

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductTypeRepository productTypeRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    private CreateProductUseCase createProduct;
    private UpdateProductUseCase updateProduct;
    private GetProductUseCase getProduct;
    private ListProductsUseCase listProducts;
    private SearchProductsUseCase searchProducts;
    private ActivateProductUseCase activateProduct;
    private DeactivateProductUseCase deactivateProduct;
    private ArchiveProductUseCase archiveProduct;
    private RestoreProductUseCase restoreProduct;
    private ChangeProductPriceUseCase changeProductPrice;
    private AdjustProductStockUseCase adjustProductStock;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        createProduct = new CreateProductUseCase(
                productRepository, productTypeRepository, productVariantRepository, clock);
        updateProduct = new UpdateProductUseCase(
                productRepository, productTypeRepository, productVariantRepository, clock);
        getProduct = new GetProductUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
        listProducts = new ListProductsUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
        searchProducts = new SearchProductsUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
        activateProduct = new ActivateProductUseCase(productRepository, clock);
        deactivateProduct = new DeactivateProductUseCase(productRepository, clock);
        archiveProduct = new ArchiveProductUseCase(productRepository, clock);
        restoreProduct = new RestoreProductUseCase(productRepository, clock);
        changeProductPrice = new ChangeProductPriceUseCase(productRepository, clock);
        adjustProductStock = new AdjustProductStockUseCase(productRepository, clock);
    }

    @Test
    void shouldCreateProductAsActive() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(productType(TYPE_ID, CATEGORY_ID)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = createProduct.execute(createCommand());

        assertEquals(CATEGORY_ID, result.categoryId());
        assertEquals(TYPE_ID, result.productTypeId());
        assertEquals(UNIT, result.presentation());
        assertEquals("Leche entera", result.name());
        assertEquals(PRICE, result.price());
        assertEquals(10, result.stock());
        assertEquals(ProductStatus.ACTIVE, result.status());
        assertEquals(NOW, result.createdAt());
        verify(productRepository).save(any(Product.class));
        verify(productTypeRepository, never()).save(any());
    }

    @Test
    void shouldDeriveCategoryFromProductTypeOnCreate() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(productType(TYPE_ID, CATEGORY_ID)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = createProduct.execute(createCommand());

        assertEquals(CATEGORY_ID, result.categoryId());
        assertEquals(TYPE_ID, result.productTypeId());
    }

    @Test
    void shouldCreateProductWithMatchingVariantAndPresentation() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(productType(TYPE_ID, CATEGORY_ID)));
        when(productVariantRepository.findById(VARIANT_ID))
                .thenReturn(Optional.of(productVariant(VARIANT_ID, TYPE_ID)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = createProduct.execute(new CreateProductCommand(
                TYPE_ID, VARIANT_ID, LITER, "7701234567890", "Leche", "Alpina", "1L", PRICE, 10, null));

        assertEquals(VARIANT_ID, result.productVariantId());
        assertEquals(LITER, result.presentation());
        assertEquals(PresentationUnit.L, result.presentation().unit());
    }

    @Test
    void shouldRejectCreateWhenVariantBelongsToAnotherType() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(productType(TYPE_ID, CATEGORY_ID)));
        when(productVariantRepository.findById(OTHER_VARIANT_ID))
                .thenReturn(Optional.of(productVariant(OTHER_VARIANT_ID, OTHER_TYPE_ID)));

        assertThrows(
                InvalidProductVariantReferenceException.class,
                () -> createProduct.execute(new CreateProductCommand(
                        TYPE_ID,
                        OTHER_VARIANT_ID,
                        UNIT,
                        "7701234567890",
                        "Leche",
                        "Alpina",
                        "1L",
                        PRICE,
                        10,
                        null)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenProductTypeIsMissing() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

        assertThrows(InvalidProductTypeReferenceException.class, () -> createProduct.execute(createCommand()));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenNameExceedsMaxLength() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(productType(TYPE_ID, CATEGORY_ID)));

        CreateProductCommand command = new CreateProductCommand(
                TYPE_ID,
                null,
                UNIT,
                "7701234567890",
                "n".repeat(Product.MAX_NAME_LENGTH + 1),
                "Alpina",
                "1L",
                PRICE,
                10,
                null);

        assertThrows(InvalidProductException.class, () -> createProduct.execute(command));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateWhenBrandExceedsMaxLength() {
        when(productRepository.findById(PRODUCT_ID))
                .thenReturn(Optional.of(product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE)));
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(productType(TYPE_ID, CATEGORY_ID)));

        UpdateProductCommand command = new UpdateProductCommand(
                PRODUCT_ID,
                TYPE_ID,
                null,
                UNIT,
                "7701234567890",
                "Leche entera",
                "b".repeat(Product.MAX_BRAND_LENGTH + 1),
                "1L",
                null);

        assertThrows(InvalidProductException.class, () -> updateProduct.execute(command));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectGetWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> getProduct.execute(new GetProductCommand(PRODUCT_ID, CatalogView.ADMIN)));
    }

    @Test
    void shouldRejectUpdateWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> updateProduct.execute(updateCommand()));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectActivateWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> activateProduct.execute(new ActivateProductCommand(PRODUCT_ID)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectDeactivateWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> deactivateProduct.execute(new DeactivateProductCommand(PRODUCT_ID)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectPriceChangeWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> changeProductPrice.execute(
                        new ChangeProductPriceCommand(PRODUCT_ID, Money.cop(new BigDecimal("5000.00")))));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldUpdateProductWithoutChangingPriceOrStock() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productTypeRepository.findById(OTHER_TYPE_ID))
                .thenReturn(Optional.of(productType(OTHER_TYPE_ID, OTHER_CATEGORY_ID)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = updateProduct.execute(new UpdateProductCommand(
                PRODUCT_ID,
                OTHER_TYPE_ID,
                null,
                UNIT,
                "770999",
                "Leche deslactosada",
                "Alquería",
                "900ml",
                null));

        assertEquals(OTHER_CATEGORY_ID, result.categoryId());
        assertEquals(OTHER_TYPE_ID, result.productTypeId());
        assertEquals("Leche deslactosada", result.name());
        assertEquals(PRICE, result.price());
        assertEquals(10, result.stock());
        assertEquals(ProductStatus.ACTIVE, result.status());
        assertEquals(existing.createdAt(), result.createdAt());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldRejectUpdateWhenProductTypeIsMissing() {
        when(productRepository.findById(PRODUCT_ID))
                .thenReturn(Optional.of(product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE)));
        when(productTypeRepository.findById(OTHER_TYPE_ID)).thenReturn(Optional.empty());

        assertThrows(
                InvalidProductTypeReferenceException.class,
                () -> updateProduct.execute(new UpdateProductCommand(
                        PRODUCT_ID, OTHER_TYPE_ID, null, UNIT, "770999", "Leche", "Alpina", null, null)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateWhenVariantBelongsToAnotherType() {
        when(productRepository.findById(PRODUCT_ID))
                .thenReturn(Optional.of(product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE)));
        when(productTypeRepository.findById(OTHER_TYPE_ID))
                .thenReturn(Optional.of(productType(OTHER_TYPE_ID, OTHER_CATEGORY_ID)));
        when(productVariantRepository.findById(VARIANT_ID))
                .thenReturn(Optional.of(productVariant(VARIANT_ID, TYPE_ID)));

        assertThrows(
                InvalidProductVariantReferenceException.class,
                () -> updateProduct.execute(new UpdateProductCommand(
                        PRODUCT_ID,
                        OTHER_TYPE_ID,
                        VARIANT_ID,
                        UNIT,
                        "770999",
                        "Leche",
                        "Alpina",
                        null,
                        null)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldActivateProduct() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.INACTIVE, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = activateProduct.execute(new ActivateProductCommand(PRODUCT_ID));

        assertEquals(ProductStatus.ACTIVE, result.status());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldRejectActivateWhenProductIsArchived() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ARCHIVED, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));

        assertThrows(
                InvalidProductException.class,
                () -> activateProduct.execute(new ActivateProductCommand(PRODUCT_ID)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateProduct() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = deactivateProduct.execute(new DeactivateProductCommand(PRODUCT_ID));

        assertEquals(ProductStatus.INACTIVE, result.status());
        assertEquals(10, result.stock());
        assertEquals(PRICE, result.price());
    }

    @Test
    void shouldArchiveProduct() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = archiveProduct.execute(new ArchiveProductCommand(PRODUCT_ID));

        assertEquals(ProductStatus.ARCHIVED, result.status());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldRestoreArchivedProductToInactive() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ARCHIVED, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = restoreProduct.execute(new RestoreProductCommand(PRODUCT_ID));

        assertEquals(ProductStatus.INACTIVE, result.status());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldRejectRestoreWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> restoreProduct.execute(new RestoreProductCommand(PRODUCT_ID)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldChangeProductPrice() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE);
        Money newPrice = Money.cop(new BigDecimal("5200.00"));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result =
                changeProductPrice.execute(new ChangeProductPriceCommand(PRODUCT_ID, newPrice));

        assertEquals(newPrice, result.price());
        assertEquals(10, result.stock());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldAdjustProductStockWithCas() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.adjustStockIfUnchanged(PRODUCT_ID, 10, 25, NOW)).thenReturn(true);

        ProductResult result = adjustProductStock.execute(new AdjustProductStockCommand(PRODUCT_ID, 25));

        assertEquals(25, result.stock());
        assertEquals(NOW, result.updatedAt());
        verify(productRepository).adjustStockIfUnchanged(PRODUCT_ID, 10, 25, NOW);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectNegativeStockAdjustment() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));

        assertThrows(
                InvalidProductException.class,
                () -> adjustProductStock.execute(new AdjustProductStockCommand(PRODUCT_ID, -1)));
        verify(productRepository, never()).adjustStockIfUnchanged(any(), anyInt(), anyInt(), any());
    }

    @Test
    void shouldRejectStockAdjustmentWhenProductIsMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> adjustProductStock.execute(new AdjustProductStockCommand(PRODUCT_ID, 25)));
    }

    @Test
    void shouldRejectStockAdjustmentWhenConcurrentChangeWins() {
        Product existing = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.ACTIVE, 10, PRICE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.adjustStockIfUnchanged(PRODUCT_ID, 10, 25, NOW)).thenReturn(false);

        assertThrows(
                ProductStockConflictException.class,
                () -> adjustProductStock.execute(new AdjustProductStockCommand(PRODUCT_ID, 25)));
    }

    @Test
    void shouldListProductsByCategoryAndStatusForAdmin() {
        Product matching = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.INACTIVE, 10, PRICE);
        when(productRepository.findByCategoryIdAndStatus(CATEGORY_ID, ProductStatus.INACTIVE))
                .thenReturn(List.of(matching));
        when(productTypeRepository.findByIds(any())).thenReturn(List.of(productType(TYPE_ID, CATEGORY_ID)));

        List<ProductResult> result = listProducts.execute(
                new ListProductsCommand(CATEGORY_ID, ProductStatus.INACTIVE, CatalogView.ADMIN));

        assertEquals(1, result.size());
        assertEquals(PRODUCT_ID, result.get(0).id());
        assertEquals(ProductStatus.INACTIVE, result.get(0).status());
    }

    @Test
    void shouldSearchProductsForAdmin() {
        Product matching = product(PRODUCT_ID, CATEGORY_ID, ProductStatus.INACTIVE, 10, PRICE);
        when(productRepository.searchByNameBrandOrBarcode("leche")).thenReturn(List.of(matching));
        when(productTypeRepository.findByIds(any())).thenReturn(List.of(productType(TYPE_ID, CATEGORY_ID)));

        List<ProductResult> result =
                searchProducts.execute(new SearchProductsCommand("leche", CatalogView.ADMIN));

        assertEquals(1, result.size());
        assertEquals(PRODUCT_ID, result.get(0).id());
    }

    @Test
    void shouldListAndSearchProductsOrderedByCatalogRules() {
        UUID idZ = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID idA = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        Product laterName = Product.create(
                idZ,
                CATEGORY_ID,
                TYPE_ID,
                null,
                Presentation.of(1, PresentationUnit.UNIT),
                null,
                "Zumo",
                null,
                null,
                PRICE,
                1,
                null,
                ProductStatus.ACTIVE,
                CREATED_AT,
                CREATED_AT);
        Product earlierName = Product.create(
                idA,
                CATEGORY_ID,
                TYPE_ID,
                null,
                Presentation.of(1, PresentationUnit.UNIT),
                null,
                "aceite",
                null,
                null,
                PRICE,
                1,
                null,
                ProductStatus.ACTIVE,
                CREATED_AT,
                CREATED_AT);
        when(productRepository.findAll()).thenReturn(List.of(laterName, earlierName));
        when(productRepository.searchByNameBrandOrBarcode("a"))
                .thenReturn(List.of(laterName, earlierName));
        when(productTypeRepository.findByIds(any())).thenReturn(List.of(productType(TYPE_ID, CATEGORY_ID)));

        List<ProductResult> listed =
                listProducts.execute(new ListProductsCommand(null, null, CatalogView.ADMIN));
        List<ProductResult> searched =
                searchProducts.execute(new SearchProductsCommand("a", CatalogView.ADMIN));

        assertEquals(List.of(idA, idZ), listed.stream().map(ProductResult::id).toList());
        assertEquals(List.of(idA, idZ), searched.stream().map(ProductResult::id).toList());
    }

    private CreateProductCommand createCommand() {
        return new CreateProductCommand(
                TYPE_ID, null, UNIT, "7701234567890", "Leche entera", "Alpina", "1L", PRICE, 10, null);
    }

    private UpdateProductCommand updateCommand() {
        return new UpdateProductCommand(
                PRODUCT_ID, TYPE_ID, null, UNIT, "7701234567890", "Leche entera", "Alpina", "1L", null);
    }

    private static ProductType productType(UUID id, UUID categoryId) {
        return ProductType.create(id, categoryId, "Lácteos", null, ProductTypeStatus.ACTIVE, CREATED_AT, CREATED_AT);
    }

    private static ProductVariant productVariant(UUID id, UUID productTypeId) {
        return ProductVariant.create(
                id, productTypeId, "Entera", null, ProductVariantStatus.ACTIVE, CREATED_AT, CREATED_AT);
    }

    private static Product product(
            UUID id, UUID categoryId, ProductStatus status, int stock, Money price) {
        return Product.create(
                id,
                categoryId,
                TYPE_ID,
                null,
                UNIT,
                "7701234567890",
                "Leche entera",
                "Alpina",
                "1L",
                price,
                stock,
                null,
                status,
                CREATED_AT,
                CREATED_AT);
    }
}
