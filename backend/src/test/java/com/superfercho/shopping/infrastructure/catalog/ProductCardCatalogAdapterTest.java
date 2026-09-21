package com.superfercho.shopping.infrastructure.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ProductCardInfo;
import com.superfercho.catalog.application.port.ProductCardQueryPort;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.favorite.FavoriteProductView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductCardCatalogAdapterTest {

    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CATEGORY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));

    @Mock
    private ProductCardQueryPort productCardQueryPort;

    @Test
    void shouldMapCatalogCardsIncludingInactiveAvailability() {
        when(productCardQueryPort.findCardsByIds(List.of(PRODUCT_ID)))
                .thenReturn(List.of(new ProductCardInfo(
                        PRODUCT_ID,
                        "Leche entera",
                        "Alpina",
                        PRICE,
                        "https://img.test/milk.png",
                        CATEGORY_ID,
                        ProductStatus.INACTIVE,
                        false)));
        ProductCardCatalogAdapter adapter = new ProductCardCatalogAdapter(productCardQueryPort);

        List<FavoriteProductView> cards = adapter.findCardsByIds(List.of(PRODUCT_ID));

        assertEquals(1, cards.size());
        FavoriteProductView card = cards.get(0);
        assertEquals(PRODUCT_ID, card.id());
        assertEquals("Leche entera", card.name());
        assertEquals("Alpina", card.brand());
        assertEquals(PRICE, card.price());
        assertEquals("https://img.test/milk.png", card.imageUrl());
        assertEquals(CATEGORY_ID, card.categoryId());
        assertEquals("INACTIVE", card.status());
        assertFalse(card.available());
        verify(productCardQueryPort).findCardsByIds(List.of(PRODUCT_ID));
    }

    @Test
    void shouldMapSellableActiveCard() {
        when(productCardQueryPort.findCardsByIds(List.of(PRODUCT_ID)))
                .thenReturn(List.of(new ProductCardInfo(
                        PRODUCT_ID, "Leche entera", "Alpina", PRICE, null, CATEGORY_ID, ProductStatus.ACTIVE, true)));
        ProductCardCatalogAdapter adapter = new ProductCardCatalogAdapter(productCardQueryPort);

        List<FavoriteProductView> cards = adapter.findCardsByIds(List.of(PRODUCT_ID));

        assertTrue(cards.get(0).available());
        assertEquals("ACTIVE", cards.get(0).status());
    }
}
