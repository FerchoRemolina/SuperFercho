package com.superfercho.platform.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.dto.CreateProductCommand;
import com.superfercho.catalog.application.dto.CreateProductVariantCommand;
import com.superfercho.catalog.application.dto.DeactivateProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.application.usecase.ArchiveProductUseCase;
import com.superfercho.catalog.application.usecase.CreateCategoryUseCase;
import com.superfercho.catalog.application.usecase.CreateProductUseCase;
import com.superfercho.catalog.application.usecase.CreateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductUseCase;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryIcon;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.application.usecase.CreateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ListDocumentsUseCase;
import com.superfercho.knowledge.application.usecase.ProcessDocumentUseCase;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import com.superfercho.platform.demo.LocalDemoDataRunner.DatasetProduct;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;

@ExtendWith(MockitoExtension.class)
class LocalDemoDataRunnerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID TYPE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Presentation UNIT = Presentation.of(1, PresentationUnit.UNIT);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductTypeRepository productTypeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private CreateCategoryUseCase createCategoryUseCase;

    @Mock
    private CreateProductUseCase createProductUseCase;

    @Mock
    private CreateProductVariantUseCase createProductVariantUseCase;

    @Mock
    private DeactivateProductUseCase deactivateProductUseCase;

    @Mock
    private ArchiveProductUseCase archiveProductUseCase;

    @Mock
    private CreateDocumentUseCase createDocumentUseCase;

    @Mock
    private ListDocumentsUseCase listDocumentsUseCase;

    @Mock
    private ProcessDocumentUseCase processDocumentUseCase;

    private LocalDemoDataRunner runner;

    @BeforeEach
    void setUp() {
        runner = newRunner(true);
    }

    private LocalDemoDataRunner newRunner(boolean enabled) {
        return new LocalDemoDataRunner(
                productRepository,
                productTypeRepository,
                categoryRepository,
                productVariantRepository,
                documentRepository,
                createCategoryUseCase,
                createProductUseCase,
                createProductVariantUseCase,
                deactivateProductUseCase,
                archiveProductUseCase,
                createDocumentUseCase,
                listDocumentsUseCase,
                processDocumentUseCase,
                CLOCK,
                enabled);
    }

    @Test
    void isRestrictedToLocalProfile() {
        Profile profile = LocalDemoDataRunner.class.getAnnotation(Profile.class);
        assertNotNull(profile);
        assertEquals(1, profile.value().length);
        assertEquals("local", profile.value()[0]);
    }

    @Test
    void skipsWhenDisabled() {
        runner = newRunner(false);

        runner.run(new DefaultApplicationArguments());

        verify(productRepository, never()).findAll();
        verify(documentRepository, never()).findAll();
        verify(createCategoryUseCase, never()).execute(any());
        verify(createDocumentUseCase, never()).execute(any());
    }

    @Test
    void skipsCatalogAndKnowledgeWhenAlreadyPresent() {
        CatalogFixture complete = catalogMatchingAllLoadableExcept(null);
        when(productRepository.findAll()).thenReturn(complete.products());
        when(productVariantRepository.findByIds(any())).thenReturn(complete.variants());
        when(documentRepository.findAll()).thenReturn(List.of(mock(KnowledgeDocument.class)));

        runner.run(new DefaultApplicationArguments());

        verify(createCategoryUseCase, never()).execute(any());
        verify(createProductUseCase, never()).execute(any());
        verify(createProductVariantUseCase, never()).execute(any());
        verify(deactivateProductUseCase, never()).execute(any());
        verify(archiveProductUseCase, never()).execute(any());
        verify(createDocumentUseCase, never()).execute(any());
    }

    @Test
    void seedsOnlyMissingDatasetIdentitiesWhenCatalogIncomplete() {
        List<DatasetProduct> loadable =
                LocalDemoDataRunner.loadableProducts(LocalDemoDataRunner.loadDatasetProducts());
        DatasetProduct sf127 = loadable.stream()
                .filter(product -> "SF-127".equals(product.id()))
                .findFirst()
                .orElseThrow();
        CatalogFixture almostComplete = catalogMatchingAllLoadableExcept("SF-127");

        UUID categoryId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID productTypeId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(productRepository.findAll()).thenReturn(almostComplete.products());
        when(productVariantRepository.findByIds(any())).thenReturn(almostComplete.variants());
        when(categoryRepository.findAll())
                .thenReturn(List.of(Category.create(
                        categoryId, sf127.category(), null, CategoryIcon.OTHER, CategoryStatus.ACTIVE, NOW, NOW)));
        when(productTypeRepository.findByCategoryId(categoryId))
                .thenReturn(List.of(ProductType.create(
                        productTypeId,
                        categoryId,
                        sf127.category(),
                        null,
                        com.superfercho.catalog.domain.model.ProductTypeStatus.ACTIVE,
                        NOW,
                        NOW)));
        when(productVariantRepository.findByProductTypeId(productTypeId)).thenReturn(List.of());
        when(createProductVariantUseCase.execute(any(CreateProductVariantCommand.class)))
                .thenAnswer(invocation -> {
                    CreateProductVariantCommand command = invocation.getArgument(0);
                    return new ProductVariantResult(
                            UUID.randomUUID(),
                            command.productTypeId(),
                            command.name(),
                            null,
                            ProductVariantStatus.ACTIVE,
                            NOW,
                            NOW);
                });
        when(createProductUseCase.execute(any(CreateProductCommand.class)))
                .thenAnswer(invocation -> {
                    CreateProductCommand command = invocation.getArgument(0);
                    return new ProductResult(
                            UUID.randomUUID(),
                            categoryId,
                            command.productTypeId(),
                            command.productVariantId(),
                            command.presentation(),
                            command.barcode(),
                            command.name(),
                            command.brand(),
                            command.description(),
                            command.price(),
                            command.stock(),
                            command.imageUrl(),
                            ProductStatus.ACTIVE,
                            NOW,
                            NOW);
                });

        runner.seedCatalogIfEmpty();

        ArgumentCaptor<CreateProductCommand> productCaptor = ArgumentCaptor.forClass(CreateProductCommand.class);
        verify(createProductUseCase, times(1)).execute(productCaptor.capture());
        verify(createCategoryUseCase, never()).execute(any());
        CreateProductCommand created = productCaptor.getValue();
        assertEquals(sf127.name(), created.name());
        assertEquals(sf127.brand(), created.brand());
        assertEquals(sf127.description(), created.description());
        assertEquals(0, sf127.price().compareTo(created.price().amount()));
        assertEquals(sf127.stock(), created.stock());
    }

    @Test
    void datasetDefinesOneHundredFiftySixProductsWithZeroPending() {
        List<DatasetProduct> dataset = LocalDemoDataRunner.loadDatasetProducts();
        List<DatasetProduct> loadable = LocalDemoDataRunner.loadableProducts(dataset);
        List<DatasetProduct> pending = LocalDemoDataRunner.pendingProducts(dataset);

        assertEquals(LocalDemoDataRunner.DATASET_DEFINED_COUNT, dataset.size());
        assertEquals(LocalDemoDataRunner.LOADABLE_COUNT, loadable.size());
        assertEquals(LocalDemoDataRunner.PENDING_COUNT, pending.size());
        assertEquals(156, loadable.size());
        assertEquals(0, pending.size());
        assertEquals(6, dataset.stream().map(DatasetProduct::category).collect(Collectors.toSet()).size());
    }

    @Test
    void pendingProductsAreEmptyForDefinitiveDataset() {
        List<DatasetProduct> pending =
                LocalDemoDataRunner.pendingProducts(LocalDemoDataRunner.loadDatasetProducts());

        assertTrue(pending.isEmpty());
        assertEquals(LocalDemoDataRunner.PENDING_COUNT, pending.size());
    }

    @ParameterizedTest
    @CsvSource({
        "1000 ml, 1000, ML",
        "150g, 150, G",
        "1.5L, 1.5, L",
        "1kg, 1, KG",
        "Unidad, 1, UNIT",
        "30 unidades, 30, UNIT",
        "10 unidades, 10, UNIT",
        "72 unidades, 72, UNIT",
        "90 unidades, 90, UNIT",
        "12 rollos, 12, ROLL",
        "4 rollos, 4, ROLL",
        "1 L, 1, L",
        "1.2 L, 1.2, L",
        "1000g, 1000, G",
        "250ml, 250, ML"
    })
    void parsePresentationMapsExactQuantityAndUnit(String raw, String quantity, PresentationUnit unit) {
        Presentation presentation = LocalDemoDataRunner.parsePresentation(raw);
        assertEquals(0, new BigDecimal(quantity).compareTo(presentation.quantity()));
        assertEquals(unit, presentation.unit());
    }

    @Test
    void parsePresentationRejectsPendingPresentations() {
        for (String pending : LocalDemoDataRunner.PENDING_PRESENTATIONS) {
            assertThrows(IllegalArgumentException.class, () -> LocalDemoDataRunner.parsePresentation(pending));
        }
    }

    @Test
    void allLoadableDatasetPresentationsAreParsable() {
        for (DatasetProduct product :
                LocalDemoDataRunner.loadableProducts(LocalDemoDataRunner.loadDatasetProducts())) {
            Presentation parsed = LocalDemoDataRunner.parsePresentation(product.presentation());
            assertNotNull(parsed);
            assertTrue(parsed.quantity().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    void seedsSixCategoriesAndOneHundredFiftySixLoadableProductsWhenCatalogEmpty() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(productVariantRepository.findByIds(any())).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(productVariantRepository.findByProductTypeId(any())).thenReturn(List.of());
        when(documentRepository.findAll()).thenReturn(List.of(mock(KnowledgeDocument.class)));
        when(createCategoryUseCase.execute(any()))
                .thenAnswer(invocation -> new CategoryResult(
                        UUID.randomUUID(),
                        "Categoria",
                        null,
                        com.superfercho.catalog.domain.model.CategoryIcon.OTHER, com.superfercho.catalog.domain.model.CategoryStatus.ACTIVE,
                        NOW,
                        NOW));
        when(productTypeRepository.save(any(ProductType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, UUID> variantIdsByKey = new HashMap<>();
        AtomicInteger variantCreates = new AtomicInteger();
        when(createProductVariantUseCase.execute(any(CreateProductVariantCommand.class)))
                .thenAnswer(invocation -> {
                    CreateProductVariantCommand command = invocation.getArgument(0);
                    String key = command.productTypeId() + "|" + command.name();
                    UUID id = variantIdsByKey.computeIfAbsent(key, ignored -> {
                        variantCreates.incrementAndGet();
                        return UUID.randomUUID();
                    });
                    return new ProductVariantResult(
                            id,
                            command.productTypeId(),
                            command.name(),
                            command.description(),
                            ProductVariantStatus.ACTIVE,
                            NOW,
                            NOW);
                });

        AtomicInteger productCreates = new AtomicInteger();
        when(createProductUseCase.execute(any(CreateProductCommand.class)))
                .thenAnswer(invocation -> {
                    CreateProductCommand command = invocation.getArgument(0);
                    productCreates.incrementAndGet();
                    return new ProductResult(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            command.productTypeId(),
                            command.productVariantId(),
                            command.presentation(),
                            command.barcode(),
                            command.name(),
                            command.brand(),
                            command.description(),
                            command.price(),
                            command.stock(),
                            command.imageUrl(),
                            ProductStatus.ACTIVE,
                            NOW,
                            NOW);
                });
        when(deactivateProductUseCase.execute(any(DeactivateProductCommand.class)))
                .thenAnswer(invocation -> sampleProductResult(ProductStatus.INACTIVE));

        runner.run(new DefaultApplicationArguments());

        List<DatasetProduct> loadable =
                LocalDemoDataRunner.loadableProducts(LocalDemoDataRunner.loadDatasetProducts());
        long expectedInactive = loadable.stream().filter(p -> "INACTIVE".equals(p.status())).count();
        long expectedArchived = loadable.stream().filter(p -> "ARCHIVED".equals(p.status())).count();
        long expectedVariantKeys = loadable.stream()
                .filter(p -> p.variant() != null)
                .map(p -> p.category() + "|" + p.variant())
                .distinct()
                .count();

        verify(createCategoryUseCase, times(6)).execute(any());
        verify(productTypeRepository, times(6)).save(any(ProductType.class));
        verify(createProductUseCase, times(LocalDemoDataRunner.LOADABLE_COUNT)).execute(any());
        verify(createProductVariantUseCase, times((int) expectedVariantKeys)).execute(any());
        verify(deactivateProductUseCase, times((int) expectedInactive)).execute(any());
        verify(archiveProductUseCase, times((int) expectedArchived)).execute(any());
        verify(createDocumentUseCase, never()).execute(any());

        assertEquals(LocalDemoDataRunner.LOADABLE_COUNT, productCreates.get());
        assertEquals(expectedVariantKeys, variantCreates.get());
        assertEquals(expectedVariantKeys, variantIdsByKey.size());
    }

    @Test
    void seedDoesNotDuplicateVariantsWithinProductType() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(productVariantRepository.findByIds(any())).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(productVariantRepository.findByProductTypeId(any())).thenReturn(List.of());
        when(createCategoryUseCase.execute(any()))
                .thenAnswer(invocation -> new CategoryResult(
                        UUID.randomUUID(),
                        "Categoria",
                        null,
                        com.superfercho.catalog.domain.model.CategoryIcon.OTHER, com.superfercho.catalog.domain.model.CategoryStatus.ACTIVE,
                        NOW,
                        NOW));
        when(productTypeRepository.save(any(ProductType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> createdVariantKeys = new HashSet<>();
        when(createProductVariantUseCase.execute(any(CreateProductVariantCommand.class)))
                .thenAnswer(invocation -> {
                    CreateProductVariantCommand command = invocation.getArgument(0);
                    String key = command.productTypeId() + "|" + command.name();
                    assertTrue(createdVariantKeys.add(key), "duplicate variant create: " + key);
                    return new ProductVariantResult(
                            UUID.randomUUID(),
                            command.productTypeId(),
                            command.name(),
                            null,
                            ProductVariantStatus.ACTIVE,
                            NOW,
                            NOW);
                });
        when(createProductUseCase.execute(any(CreateProductCommand.class)))
                .thenAnswer(invocation -> {
                    CreateProductCommand command = invocation.getArgument(0);
                    return new ProductResult(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            command.productTypeId(),
                            command.productVariantId(),
                            command.presentation(),
                            command.barcode(),
                            command.name(),
                            command.brand(),
                            command.description(),
                            command.price(),
                            command.stock(),
                            command.imageUrl(),
                            ProductStatus.ACTIVE,
                            NOW,
                            NOW);
                });
        when(deactivateProductUseCase.execute(any())).thenReturn(sampleProductResult(ProductStatus.INACTIVE));

        runner.seedCatalogIfEmpty();

        ArgumentCaptor<CreateProductCommand> productCaptor = ArgumentCaptor.forClass(CreateProductCommand.class);
        verify(createProductUseCase, times(LocalDemoDataRunner.LOADABLE_COUNT)).execute(productCaptor.capture());

        long nullVariantProducts = LocalDemoDataRunner.loadableProducts(LocalDemoDataRunner.loadDatasetProducts())
                .stream()
                .filter(p -> p.variant() == null)
                .count();
        long nullVariantCommands =
                productCaptor.getAllValues().stream().filter(c -> c.productVariantId() == null).count();
        assertEquals(nullVariantProducts, nullVariantCommands);
        assertFalse(productCaptor.getAllValues().stream().allMatch(c -> c.productVariantId() == null));
    }

    @Test
    void seedPreservesImageUrlBarcodeAndAppliesStatuses() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(productVariantRepository.findByIds(any())).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(productVariantRepository.findByProductTypeId(any())).thenReturn(List.of());
        when(createCategoryUseCase.execute(any()))
                .thenAnswer(invocation -> new CategoryResult(
                        UUID.randomUUID(),
                        "Categoria",
                        null,
                        com.superfercho.catalog.domain.model.CategoryIcon.OTHER, com.superfercho.catalog.domain.model.CategoryStatus.ACTIVE,
                        NOW,
                        NOW));
        when(productTypeRepository.save(any(ProductType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(createProductVariantUseCase.execute(any()))
                .thenAnswer(invocation -> {
                    CreateProductVariantCommand command = invocation.getArgument(0);
                    return new ProductVariantResult(
                            UUID.randomUUID(),
                            command.productTypeId(),
                            command.name(),
                            null,
                            ProductVariantStatus.ACTIVE,
                            NOW,
                            NOW);
                });
        when(createProductUseCase.execute(any()))
                .thenAnswer(invocation -> {
                    CreateProductCommand command = invocation.getArgument(0);
                    return new ProductResult(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            command.productTypeId(),
                            command.productVariantId(),
                            command.presentation(),
                            command.barcode(),
                            command.name(),
                            command.brand(),
                            command.description(),
                            command.price(),
                            command.stock(),
                            command.imageUrl(),
                            ProductStatus.ACTIVE,
                            NOW,
                            NOW);
                });
        when(deactivateProductUseCase.execute(any())).thenReturn(sampleProductResult(ProductStatus.INACTIVE));

        runner.seedCatalogIfEmpty();

        ArgumentCaptor<CreateProductCommand> productCaptor = ArgumentCaptor.forClass(CreateProductCommand.class);
        verify(createProductUseCase, times(LocalDemoDataRunner.LOADABLE_COUNT)).execute(productCaptor.capture());

        List<DatasetProduct> loadable =
                LocalDemoDataRunner.loadableProducts(LocalDemoDataRunner.loadDatasetProducts());

        DatasetProduct withImage = loadable.stream()
                .filter(p -> p.imageUrl() != null && p.barcode() != null)
                .findFirst()
                .orElseThrow();
        DatasetProduct nullImage = loadable.stream()
                .filter(p -> p.imageUrl() == null)
                .findFirst()
                .orElseThrow();

        CreateProductCommand withImageCommand = findCommand(productCaptor.getAllValues(), withImage);
        CreateProductCommand nullImageCommand = findCommand(productCaptor.getAllValues(), nullImage);

        assertEquals(withImage.imageUrl(), withImageCommand.imageUrl());
        assertEquals(withImage.barcode(), withImageCommand.barcode());
        assertNull(nullImageCommand.imageUrl());
        assertEquals(nullImage.barcode(), nullImageCommand.barcode());

        long expectedActive = loadable.stream().filter(p -> "ACTIVE".equals(p.status())).count();
        long expectedInactive = loadable.stream().filter(p -> "INACTIVE".equals(p.status())).count();
        long expectedArchived = loadable.stream().filter(p -> "ARCHIVED".equals(p.status())).count();
        assertEquals(144, expectedActive);
        assertEquals(12, expectedInactive);
        assertEquals(0, expectedArchived);
        assertEquals(
                LocalDemoDataRunner.LOADABLE_COUNT, expectedActive + expectedInactive + expectedArchived);
    }

    private static CreateProductCommand findCommand(List<CreateProductCommand> commands, DatasetProduct product) {
        Presentation expected = LocalDemoDataRunner.parsePresentation(product.presentation());
        return commands.stream()
                .filter(c -> c.name().equals(product.name())
                        && ObjectsEquals(c.brand(), product.brand())
                        && c.presentation().equals(expected)
                        && ObjectsEquals(c.barcode(), product.barcode())
                        && ObjectsEquals(c.imageUrl(), product.imageUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("command not found for " + product.id()));
    }

    private static boolean ObjectsEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private static ProductResult sampleProductResult(ProductStatus status) {
        return new ProductResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TYPE_ID,
                null,
                UNIT,
                "770",
                "Producto",
                "Marca",
                "Desc",
                Money.cop(new BigDecimal("1000.00")),
                status == ProductStatus.ARCHIVED ? 0 : 1,
                null,
                status,
                NOW,
                NOW);
    }

    private static CatalogFixture catalogMatchingAllLoadableExcept(String excludedId) {
        List<Product> products = new java.util.ArrayList<>();
        List<ProductVariant> variants = new java.util.ArrayList<>();
        for (DatasetProduct dataset :
                LocalDemoDataRunner.loadableProducts(LocalDemoDataRunner.loadDatasetProducts())) {
            if (excludedId != null && excludedId.equals(dataset.id())) {
                continue;
            }
            UUID variantId = null;
            boolean needsVariantForMatch = (dataset.description() == null || dataset.description().isBlank())
                    && dataset.variant() != null
                    && (dataset.barcode() == null || dataset.barcode().isBlank());
            if (needsVariantForMatch) {
                variantId = UUID.randomUUID();
                variants.add(ProductVariant.create(
                        variantId,
                        TYPE_ID,
                        dataset.variant(),
                        null,
                        ProductVariantStatus.ACTIVE,
                        NOW,
                        NOW));
            }
            products.add(productMatchingDataset(dataset, variantId));
        }
        return new CatalogFixture(List.copyOf(products), List.copyOf(variants));
    }

    private static Product productMatchingDataset(DatasetProduct dataset, UUID variantId) {
        ProductStatus status = ProductStatus.valueOf(dataset.status());
        int stock = status == ProductStatus.ARCHIVED ? 0 : dataset.stock();
        return Product.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TYPE_ID,
                variantId,
                LocalDemoDataRunner.parsePresentation(dataset.presentation()),
                dataset.barcode(),
                dataset.name(),
                dataset.brand(),
                dataset.description(),
                Money.cop(dataset.price()),
                stock,
                dataset.imageUrl(),
                status,
                NOW,
                NOW);
    }

    private record CatalogFixture(List<Product> products, List<ProductVariant> variants) {
    }
}
