package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ProductPriceInfo;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindProductPriceUseCaseTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Money PRICE = Money.cop(new BigDecimal("4500.00"));

    @Mock
    private ProductRepository productRepository;

    @Test
    void shouldReturnProductIdAndCurrentPrice() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));

        Optional<ProductPriceInfo> result = new FindProductPriceUseCase(productRepository).findById(PRODUCT_ID);

        assertTrue(result.isPresent());
        assertEquals(PRODUCT_ID, result.get().productId());
        assertEquals(PRICE, result.get().currentPrice());
    }

    @Test
    void shouldReturnEmptyWhenProductDoesNotExist() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        Optional<ProductPriceInfo> result = new FindProductPriceUseCase(productRepository).findById(PRODUCT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenProductIsInactive() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(ProductStatus.INACTIVE)));

        Optional<ProductPriceInfo> result = new FindProductPriceUseCase(productRepository).findById(PRODUCT_ID);

        assertTrue(result.isEmpty());
    }

    private static Product product() {
        return product(ProductStatus.ACTIVE);
    }

    private static Product product(ProductStatus status) {
        return Product.create(
                PRODUCT_ID,
                CATEGORY_ID,
                "7701234567890",
                "Leche entera",
                "Alpina",
                "1L",
                PRICE,
                10,
                null,
                status,
                CREATED_AT,
                CREATED_AT);
    }
}
