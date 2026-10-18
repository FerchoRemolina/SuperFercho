package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.ProductSales;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.application.port.SalesPort;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryIcon;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * "Los más vendidos" del Home: ventana móvil 7 → 30 → 90 días → histórico,
 * sin duplicados, solo visibles/disponibles, diversidad por categoría que
 * preserva el ranking, y menos de 16 elegibles se devuelven completos.
 */
@ExtendWith(MockitoExtension.class)
class ListBestSellersUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-09-01T00:00:00Z");
    private static final String BEBIDAS = "Bebidas";
    private static final String LACTEOS = "Lácteos";
    private static final UUID CATEGORY_BEBIDAS =
            UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_LACTEOS =
            UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");

    @Mock
    private SalesPort salesPort;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductTypeRepository productTypeRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    private ListBestSellersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListBestSellersUseCase(
                salesPort,
                productRepository,
                categoryRepository,
                productTypeRepository,
                productVariantRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void ranksByUnitsSoldUsingSevenDayWindowFirst() {
        Product top = availableProduct("top", CATEGORY_BEBIDAS, 10);
        Product second = availableProduct("second", CATEGORY_LACTEOS, 10);
        stubCatalogLookup(List.of(top, second));
        when(salesPort.topProductsByQuantity(
                        eq(NOW.minus(Duration.ofDays(7))), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(List.of(
                        new ProductSales(top.id(), 40), new ProductSales(second.id(), 12)));

        List<ProductResult> result = useCase.execute(null);

        verify(salesPort).topProductsByQuantity(
                eq(NOW.minus(Duration.ofDays(7))), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT));
        assertEquals(List.of(top.id(), second.id()), result.stream().map(ProductResult::id).toList());
        assertEquals(ProductStatus.ACTIVE, result.get(0).status());
    }

    @Test
    void fallsBackTo30DaysWhenSevenDaysCannotFillTheLimit() {
        Product recent = availableProduct("recent", CATEGORY_BEBIDAS, 10);
        Product older = availableProduct("older", CATEGORY_LACTEOS, 10);
        stubCatalogLookup(List.of(recent, older));
        when(salesPort.topProductsByQuantity(
                        eq(NOW.minus(Duration.ofDays(7))), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(List.of(new ProductSales(recent.id(), 3)));
        when(salesPort.topProductsByQuantity(
                        eq(NOW.minus(Duration.ofDays(30))), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(List.of(
                        new ProductSales(recent.id(), 9), new ProductSales(older.id(), 7)));

        List<ProductResult> result = useCase.execute(2);

        verify(salesPort).topProductsByQuantity(
                eq(NOW.minus(Duration.ofDays(30))), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT));
        // El reciente (ventana 7 días) conserva su posición; sin duplicados.
        assertEquals(List.of(recent.id(), older.id()), result.stream().map(ProductResult::id).toList());
    }

    @Test
    void fallsBackTo90DaysAndThenHistoryWhenWindowsAreEmpty() {
        Product historical = availableProduct("historical", CATEGORY_LACTEOS, 10);
        when(salesPort.topProductsByQuantity(any(), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(List.of());
        when(salesPort.topProductsByQuantity(eq(Instant.EPOCH), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(List.of(new ProductSales(historical.id(), 2)));
        when(productRepository.findByIds(anyCollection())).thenReturn(List.of(historical));
        stubCatalogLookup(List.of(historical));

        List<ProductResult> result = useCase.execute(16);

        verify(salesPort).topProductsByQuantity(
                eq(Instant.EPOCH), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT));
        assertEquals(List.of(historical.id()), result.stream().map(ProductResult::id).toList());
    }

    @Test
    void excludesOutOfStockAndNonVisibleProducts() {
        // Con stock 0 (visible) y con tipo INACTIVE (no visible): ninguno entra.
        Product outOfStock = availableProduct("out", CATEGORY_LACTEOS, 0);
        Product hiddenType = availableProduct("hidden", CATEGORY_BEBIDAS, 10);
        when(categoryRepository.findByIds(anyCollection()))
                .thenReturn(List.of(category(CATEGORY_BEBIDAS), category(CATEGORY_LACTEOS)));
        when(productTypeRepository.findByIds(anyCollection()))
                .thenReturn(List.of(
                        productType(BEBIDAS, CATEGORY_BEBIDAS, ProductTypeStatus.INACTIVE),
                        productType(LACTEOS, CATEGORY_LACTEOS, ProductTypeStatus.ACTIVE)));
        when(salesPort.topProductsByQuantity(any(), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(List.of(
                        new ProductSales(outOfStock.id(), 50),
                        new ProductSales(hiddenType.id(), 40)));
        when(productRepository.findByIds(anyCollection()))
                .thenReturn(List.of(outOfStock, hiddenType));

        List<ProductResult> result = useCase.execute(16);

        assertEquals(List.of(), result.stream().map(ProductResult::id).toList());
    }

    @Test
    void diversityCapsOneCategoryThenRefillsFromRanking() {
        // 6 productos: 5 de Bebidas (ranking 1-5) y 1 de Lácteos (ranking 6).
        Product b1 = availableProduct("b1", CATEGORY_BEBIDAS, 10);
        Product b2 = availableProduct("b2", CATEGORY_BEBIDAS, 10);
        Product b3 = availableProduct("b3", CATEGORY_BEBIDAS, 10);
        Product b4 = availableProduct("b4", CATEGORY_BEBIDAS, 10);
        Product b5 = availableProduct("b5", CATEGORY_BEBIDAS, 10);
        Product lacteo = availableProduct("lacteo", CATEGORY_LACTEOS, 10);
        stubCatalogLookup(List.of(b1, b2, b3, b4, b5, lacteo));
        when(salesPort.topProductsByQuantity(
                        eq(NOW.minus(Duration.ofDays(7))), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(List.of(
                        new ProductSales(b1.id(), 60),
                        new ProductSales(b2.id(), 50),
                        new ProductSales(b3.id(), 40),
                        new ProductSales(b4.id(), 30),
                        new ProductSales(b5.id(), 20),
                        new ProductSales(lacteo.id(), 10)));

        List<ProductResult> result = useCase.execute(6);

        // Tope de 4 por categoría en primera pasada; el cupo restante se llena
        // con el excedente en su orden original: el ranking se preserva.
        assertEquals(
                List.of(b1.id(), b2.id(), b3.id(), b4.id(), lacteo.id(), b5.id()),
                result.stream().map(ProductResult::id).toList());
    }

    @Test
    void returnsAllEligibleWhenFewerThanLimit() {
        Product only = availableProduct("only", CATEGORY_BEBIDAS, 10);
        stubCatalogLookup(List.of(only));
        when(salesPort.topProductsByQuantity(
                        eq(NOW.minus(Duration.ofDays(7))), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(List.of(new ProductSales(only.id(), 1)));

        List<ProductResult> result = useCase.execute(16);

        assertEquals(1, result.size());
        assertEquals("Producto only", result.get(0).name());
    }

    @Test
    void returnsExactlyTheRequestedLimitWhenEnoughEligibleProductsExist() {
        List<Product> products = new ArrayList<>();
        List<ProductSales> sales = new ArrayList<>();
        for (int index = 1; index <= 20; index++) {
            Product product = availableProduct("p" + index, CATEGORY_BEBIDAS, 10);
            products.add(product);
            sales.add(new ProductSales(product.id(), 100 - index));
        }
        stubCatalogLookup(products);
        when(salesPort.topProductsByQuantity(
                        eq(NOW.minus(Duration.ofDays(7))), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(sales);

        List<ProductResult> result = useCase.execute(16);

        assertEquals(16, result.size());
    }

    @Test
    void capsAbusiveLimitsToTheMaximumAllowed() {
        List<Product> products = new ArrayList<>();
        List<ProductSales> sales = new ArrayList<>();
        for (int index = 1; index <= ListBestSellersUseCase.MAX_LIMIT + 8; index++) {
            Product product = availableProduct("p" + index, CATEGORY_BEBIDAS, 10);
            products.add(product);
            sales.add(new ProductSales(product.id(), 1000 - index));
        }
        stubCatalogLookup(products);
        when(salesPort.topProductsByQuantity(
                        eq(NOW.minus(Duration.ofDays(7))), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(sales);

        List<ProductResult> result = useCase.execute(999);

        assertEquals(ListBestSellersUseCase.MAX_LIMIT, result.size());
    }

    @Test
    void resultCarriesCommercialTaxonomyForTheStorefront() {
        Product top = availableProduct("top", CATEGORY_BEBIDAS, 10);
        stubCatalogLookup(List.of(top));
        when(salesPort.topProductsByQuantity(
                        eq(NOW.minus(Duration.ofDays(7))), eq(NOW), eq(ListBestSellersUseCase.CANDIDATE_LIMIT)))
                .thenReturn(List.of(new ProductSales(top.id(), 5)));

        List<ProductResult> result = useCase.execute(null);

        assertEquals(BEBIDAS, result.get(0).productTypeName());
        assertNull(result.get(0).productVariantName());
    }

    private void stubCatalogLookup(List<Product> products) {
        // Registro tolerante: el lookup por ventana puede repetir consultas.
        lenient().when(categoryRepository.findByIds(anyCollection()))
                .thenReturn(List.of(category(CATEGORY_BEBIDAS), category(CATEGORY_LACTEOS)));
        lenient().when(productTypeRepository.findByIds(anyCollection()))
                .thenReturn(List.of(
                        productType(BEBIDAS, CATEGORY_BEBIDAS, ProductTypeStatus.ACTIVE),
                        productType(LACTEOS, CATEGORY_LACTEOS, ProductTypeStatus.ACTIVE)));
        lenient().when(productVariantRepository.findByIds(anyCollection())).thenReturn(List.of());
        lenient().when(productRepository.findByIds(anyCollection())).thenReturn(products);
    }

    private String categoryName(UUID categoryId) {
        return categoryId.equals(CATEGORY_BEBIDAS) ? BEBIDAS : LACTEOS;
    }

    private Product availableProduct(String suffix, UUID categoryId, int stock) {
        return Product.create(
                UUID.nameUUIDFromBytes(suffix.getBytes()),
                categoryId,
                UUID.nameUUIDFromBytes(categoryName(categoryId).getBytes()),
                null,
                Presentation.of(1, PresentationUnit.UNIT),
                null,
                "Producto " + suffix,
                null,
                null,
                Money.cop(new BigDecimal("4500.00")),
                stock,
                null,
                ProductStatus.ACTIVE,
                CREATED_AT,
                NOW);
    }

    private Category category(UUID id) {
        return Category.create(
                id,
                categoryName(id),
                null,
                CategoryIcon.OTHER,
                CategoryStatus.ACTIVE,
                CREATED_AT,
                NOW);
    }

    private ProductType productType(String name, UUID categoryId, ProductTypeStatus status) {
        return ProductType.create(
                UUID.nameUUIDFromBytes(name.getBytes()),
                categoryId,
                name,
                null,
                status,
                CREATED_AT,
                NOW);
    }
}
