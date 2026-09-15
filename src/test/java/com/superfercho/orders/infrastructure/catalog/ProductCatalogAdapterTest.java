package com.superfercho.orders.infrastructure.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.orders.application.dto.AvailabilityResult;
import com.superfercho.orders.application.dto.ProductCatalogInfo;
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

@ExtendWith(MockitoExtension.class)
class ProductCatalogAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID MILK_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BREAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Money MILK_PRICE = Money.cop(new BigDecimal("10.50"));

    @Mock
    private ProductRepository productRepository;

    private ProductCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProductCatalogAdapter(productRepository);
    }

    @Test
    void shouldMapExistingProduct() {
        when(productRepository.findById(MILK_ID)).thenReturn(Optional.of(product(MILK_ID, ProductStatus.ACTIVE, 10)));

        Optional<ProductCatalogInfo> result = adapter.getProduct(MILK_ID);

        assertTrue(result.isPresent());
        ProductCatalogInfo info = result.get();
        assertEquals(MILK_ID, info.productId());
        assertEquals("Leche entera", info.name());
        assertEquals(MILK_PRICE, info.currentPrice());
        assertTrue(info.available());
        assertTrue(info.active());
        verify(productRepository).findById(MILK_ID);
    }

    @Test
    void shouldReturnEmptyWhenProductDoesNotExist() {
        when(productRepository.findById(MILK_ID)).thenReturn(Optional.empty());

        assertTrue(adapter.getProduct(MILK_ID).isEmpty());
        verify(productRepository).findById(MILK_ID);
    }

    @Test
    void shouldMapInactiveProductWithoutHidingIt() {
        when(productRepository.findById(MILK_ID)).thenReturn(Optional.of(product(MILK_ID, ProductStatus.INACTIVE, 10)));

        ProductCatalogInfo info = adapter.getProduct(MILK_ID).orElseThrow();

        assertFalse(info.active());
        assertTrue(info.available());
    }

    @Test
    void shouldMapZeroStockAsUnavailableWithoutChangingActive() {
        when(productRepository.findById(MILK_ID)).thenReturn(Optional.of(product(MILK_ID, ProductStatus.ACTIVE, 0)));

        ProductCatalogInfo info = adapter.getProduct(MILK_ID).orElseThrow();

        assertTrue(info.active());
        assertFalse(info.available());
    }

    @Test
    void shouldReportAvailableWhenStockCoversRequestedQuantities() {
        when(productRepository.findById(MILK_ID)).thenReturn(Optional.of(product(MILK_ID, ProductStatus.ACTIVE, 10)));
        when(productRepository.findById(BREAD_ID)).thenReturn(Optional.of(product(BREAD_ID, ProductStatus.ACTIVE, 2)));

        AvailabilityResult result =
                adapter.checkAvailability(List.of(new StockQuantity(MILK_ID, 2), new StockQuantity(BREAD_ID, 1)));

        assertTrue(result.allAvailable());
        assertTrue(result.unavailableProductIds().isEmpty());
    }

    @Test
    void shouldReportUnavailableWhenProductIsMissing() {
        when(productRepository.findById(MILK_ID)).thenReturn(Optional.empty());

        AvailabilityResult result = adapter.checkAvailability(List.of(new StockQuantity(MILK_ID, 1)));

        assertFalse(result.allAvailable());
        assertEquals(List.of(MILK_ID), result.unavailableProductIds());
    }

    @Test
    void shouldReportUnavailableWhenStockIsInsufficient() {
        when(productRepository.findById(MILK_ID)).thenReturn(Optional.of(product(MILK_ID, ProductStatus.ACTIVE, 1)));

        AvailabilityResult result = adapter.checkAvailability(List.of(new StockQuantity(MILK_ID, 2)));

        assertFalse(result.allAvailable());
        assertEquals(List.of(MILK_ID), result.unavailableProductIds());
    }

    @Test
    void shouldNotTreatInactiveStatusAsInsufficientStock() {
        when(productRepository.findById(MILK_ID)).thenReturn(Optional.of(product(MILK_ID, ProductStatus.INACTIVE, 5)));

        AvailabilityResult result = adapter.checkAvailability(List.of(new StockQuantity(MILK_ID, 2)));

        assertTrue(result.allAvailable());
    }

    private static Product product(UUID id, ProductStatus status, int stock) {
        return Product.create(
                id,
                CATEGORY_ID,
                "7701234567890",
                "Leche entera",
                "Alpina",
                "1L whole milk",
                MILK_PRICE,
                stock,
                null,
                status,
                NOW,
                NOW);
    }
}
