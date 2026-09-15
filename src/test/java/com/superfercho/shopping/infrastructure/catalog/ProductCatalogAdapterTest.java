package com.superfercho.shopping.infrastructure.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.catalog.application.dto.ProductPriceInfo;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductCatalogAdapterTest {

    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));

    @Test
    void shouldMapCatalogPriceToShoppingInfo() {
        ProductCatalogAdapter adapter = new ProductCatalogAdapter(
                productId -> Optional.of(new ProductPriceInfo(productId, PRICE)));

        Optional<ProductCatalogInfo> result = adapter.getProduct(PRODUCT_ID);

        assertTrue(result.isPresent());
        assertEquals(PRODUCT_ID, result.get().productId());
        assertEquals(PRICE, result.get().currentPrice());
    }

    @Test
    void shouldReturnEmptyWhenCatalogHasNoProduct() {
        ProductCatalogAdapter adapter = new ProductCatalogAdapter(productId -> Optional.empty());

        assertTrue(adapter.getProduct(PRODUCT_ID).isEmpty());
    }
}
