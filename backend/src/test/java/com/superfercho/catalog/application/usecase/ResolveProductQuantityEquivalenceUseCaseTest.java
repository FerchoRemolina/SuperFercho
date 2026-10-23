package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.QuantityEquivalenceCommand;
import com.superfercho.catalog.application.dto.QuantityEquivalenceResult;
import com.superfercho.catalog.application.dto.QuantityEquivalenceStatus;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.exception.InvalidPresentationException;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResolveProductQuantityEquivalenceUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_PRODUCT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TYPE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Money PRICE = Money.cop(new BigDecimal("4500.00"));

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductTypeRepository productTypeRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    private ResolveProductQuantityEquivalenceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResolveProductQuantityEquivalenceUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
    }

    @Test
    void exactMatchOneLitreAgainstFiveHundredMillilitres() {
        givenSellable(product(Presentation.of(500, PresentationUnit.ML), 8));

        QuantityEquivalenceResult result = useCase.execute(command(new BigDecimal("1"), PresentationUnit.L));

        assertEquals(QuantityEquivalenceStatus.EXACT, result.status());
        assertTrue(result.exactMatch());
        assertEquals(2, result.exactPackageCount());
        assertEquals(0, new BigDecimal("1000.000").compareTo(result.exactPhysicalTotalBase()));
        assertEquals(PRICE, result.currentPackagePrice());
        assertEquals("AVAILABLE", result.availability());
        assertNull(result.lowerAlternative());
        assertNull(result.upperAlternative());
        assertTrue(result.sellable());
        assertTrue(result.summary().contains("not a cart mutation"));
    }

    @Test
    void inexactTwoPointThreeKilogramsAgainstOneKilogram() {
        givenSellable(product(Presentation.of(1, PresentationUnit.KG), 10));

        QuantityEquivalenceResult result = useCase.execute(command(new BigDecimal("2.3"), PresentationUnit.KG));

        assertEquals(QuantityEquivalenceStatus.INEXACT, result.status());
        assertFalse(result.exactMatch());
        assertNull(result.exactPackageCount());
        assertEquals(2, result.lowerAlternative().packageCount());
        assertEquals(3, result.upperAlternative().packageCount());
        assertEquals("AVAILABLE", result.lowerAlternative().availability());
        assertTrue(result.summary().contains("NO EXACT MATCH"));
    }

    @Test
    void inexactTwoPointThreeKilogramsAgainstFiveHundredGrams() {
        givenSellable(product(Presentation.of(500, PresentationUnit.G), 10));

        QuantityEquivalenceResult result = useCase.execute(command(new BigDecimal("2.3"), PresentationUnit.KG));

        assertEquals(4, result.lowerAlternative().packageCount());
        assertEquals(5, result.upperAlternative().packageCount());
        assertNull(result.exactPackageCount());
    }

    @Test
    void inexactTwoPointThreeKilogramsAgainstSevenHundredFiftyGrams() {
        givenSellable(product(Presentation.of(750, PresentationUnit.G), 10));

        QuantityEquivalenceResult result = useCase.execute(command(new BigDecimal("2.3"), PresentationUnit.KG));

        assertEquals(3, result.lowerAlternative().packageCount());
        assertEquals(4, result.upperAlternative().packageCount());
    }

    @Test
    void incompatibleMassAgainstVolume() {
        givenSellable(product(Presentation.of(500, PresentationUnit.ML), 10));

        QuantityEquivalenceResult result = useCase.execute(command(new BigDecimal("1"), PresentationUnit.KG));

        assertEquals(QuantityEquivalenceStatus.INCOMPATIBLE_UNITS, result.status());
        assertFalse(result.exactMatch());
        assertNull(result.exactPackageCount());
        assertTrue(result.sellable());
        assertEquals(PresentationUnit.ML, result.presentation().unit());
    }

    @Test
    void unitPresentationIsNotConvertedFromKilograms() {
        givenSellable(product(Presentation.of(1, PresentationUnit.UNIT), 10));

        QuantityEquivalenceResult result = useCase.execute(command(new BigDecimal("3"), PresentationUnit.KG));

        assertEquals(QuantityEquivalenceStatus.INCOMPATIBLE_UNITS, result.status());
        assertNull(result.exactPackageCount());
    }

    @Test
    void packBoxAndRollPresentationsAreNotConverted() {
        givenSellable(product(Presentation.of(2, PresentationUnit.PACK), 10));
        assertEquals(
                QuantityEquivalenceStatus.INCOMPATIBLE_UNITS,
                useCase.execute(command(new BigDecimal("1"), PresentationUnit.KG)).status());

        givenSellable(product(Presentation.of(1, PresentationUnit.BOX), 10));
        assertEquals(
                QuantityEquivalenceStatus.INCOMPATIBLE_UNITS,
                useCase.execute(command(new BigDecimal("1"), PresentationUnit.L)).status());

        givenSellable(product(Presentation.of(12, PresentationUnit.ROLL), 10));
        assertEquals(
                QuantityEquivalenceStatus.INCOMPATIBLE_UNITS,
                useCase.execute(command(new BigDecimal("1"), PresentationUnit.KG)).status());
    }

    @Test
    void rejectsZeroNegativeAndExcessScale() {
        assertThrows(
                InvalidPresentationException.class,
                () -> useCase.execute(command(BigDecimal.ZERO, PresentationUnit.KG)));
        assertThrows(
                InvalidPresentationException.class,
                () -> useCase.execute(command(new BigDecimal("-1"), PresentationUnit.KG)));
        assertThrows(
                InvalidPresentationException.class,
                () -> useCase.execute(command(new BigDecimal("1.2345"), PresentationUnit.KG)));
    }

    @Test
    void missingProductIsNotPurchasable() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        QuantityEquivalenceResult result = useCase.execute(command(new BigDecimal("1"), PresentationUnit.L));

        assertEquals(QuantityEquivalenceStatus.NOT_AVAILABLE, result.status());
        assertFalse(result.sellable());
        assertEquals("PRODUCT_NOT_FOUND", result.unavailabilityReason());
        assertNull(result.currentPackagePrice());
        assertNull(result.exactPackageCount());
    }

    @Test
    void inactiveProductIsNotPurchasable() {
        givenProductOnly(product(Presentation.of(1, PresentationUnit.KG), 10, ProductStatus.INACTIVE));
        givenCategory(CategoryStatus.ACTIVE);
        givenType(ProductTypeStatus.ACTIVE);

        QuantityEquivalenceResult result = useCase.execute(command(new BigDecimal("1"), PresentationUnit.KG));

        assertEquals(QuantityEquivalenceStatus.NOT_AVAILABLE, result.status());
        assertFalse(result.sellable());
        assertEquals("NOT_SELLABLE", result.unavailabilityReason());
        assertNull(result.currentPackagePrice());
        assertNull(result.exactPackageCount());
    }

    @Test
    void inactiveCategoryMakesProductNotSellable() {
        givenProductOnly(product(Presentation.of(1, PresentationUnit.KG), 10, ProductStatus.ACTIVE));
        givenCategory(CategoryStatus.INACTIVE);
        givenType(ProductTypeStatus.ACTIVE);

        QuantityEquivalenceResult result = useCase.execute(command(new BigDecimal("1"), PresentationUnit.KG));

        assertEquals(QuantityEquivalenceStatus.NOT_AVAILABLE, result.status());
        assertFalse(result.sellable());
    }

    @Test
    void insufficientStockIsReportedWithoutInventingAvailability() {
        givenSellable(product(Presentation.of(500, PresentationUnit.ML), 1));

        QuantityEquivalenceResult result = useCase.execute(command(new BigDecimal("1"), PresentationUnit.L));

        assertTrue(result.exactMatch());
        assertEquals(2, result.exactPackageCount());
        assertEquals("INSUFFICIENT_STOCK", result.availability());
        assertEquals(1, result.catalogStock());
    }

    @Test
    void doesNotLeakAnotherProduct() {
        when(productRepository.findById(OTHER_PRODUCT_ID)).thenReturn(Optional.empty());

        QuantityEquivalenceResult other =
                useCase.execute(new QuantityEquivalenceCommand(OTHER_PRODUCT_ID, BigDecimal.ONE, PresentationUnit.KG));

        assertEquals(QuantityEquivalenceStatus.NOT_AVAILABLE, other.status());
        assertEquals(OTHER_PRODUCT_ID, other.productId());
        assertNull(other.currentPackagePrice());
        assertNull(other.productName());
    }

    @Test
    void priceUnavailableFactoryIsNotPurchasable() {
        QuantityEquivalenceResult result = QuantityEquivalenceResult.priceUnavailable(
                PRODUCT_ID,
                "Arroz Blanco",
                Presentation.of(1, PresentationUnit.KG),
                new BigDecimal("2.3"),
                PresentationUnit.KG,
                10);

        assertEquals(QuantityEquivalenceStatus.NOT_AVAILABLE, result.status());
        assertFalse(result.sellable());
        assertEquals("PRICE_UNAVAILABLE", result.unavailabilityReason());
        assertNull(result.currentPackagePrice());
        assertNull(result.exactPackageCount());
    }

    private void givenSellable(Product product) {
        givenProductOnly(product);
        givenCategory(CategoryStatus.ACTIVE);
        givenType(ProductTypeStatus.ACTIVE);
    }

    private void givenProductOnly(Product product) {
        when(productRepository.findById(product.id())).thenReturn(Optional.of(product));
    }

    private void givenCategory(CategoryStatus status) {
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(Category.create(
                        CATEGORY_ID, "Despensa", null, CategoryIcon.GROCERY, status, NOW, NOW)));
    }

    private void givenType(ProductTypeStatus status) {
        when(productTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(ProductType.create(TYPE_ID, CATEGORY_ID, "Arroz", null, status, NOW, NOW)));
    }

    private static QuantityEquivalenceCommand command(BigDecimal quantity, PresentationUnit unit) {
        return new QuantityEquivalenceCommand(PRODUCT_ID, quantity, unit);
    }

    private static Product product(Presentation presentation, int stock) {
        return product(presentation, stock, ProductStatus.ACTIVE);
    }

    private static Product product(Presentation presentation, int stock, ProductStatus status) {
        return Product.create(
                PRODUCT_ID,
                CATEGORY_ID,
                TYPE_ID,
                null,
                presentation,
                "7701",
                "Arroz Blanco",
                "Diana",
                "paquete",
                PRICE,
                stock,
                null,
                status,
                NOW,
                NOW);
    }
}
