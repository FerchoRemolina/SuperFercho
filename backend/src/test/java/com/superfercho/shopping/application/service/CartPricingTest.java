package com.superfercho.shopping.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartPricingTest {

    private static final Instant NOW = Instant.parse("2026-04-01T10:00:00Z");
    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MILK_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BREAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final Money HISTORICAL_MILK = Money.cop(new BigDecimal("10.50"));
    private static final Money CURRENT_MILK = Money.cop(new BigDecimal("12.00"));
    private static final Money CURRENT_BREAD = Money.cop(new BigDecimal("3.00"));

    @Mock
    private ProductCatalogPort catalog;

    @Test
    void emptyCartHasZeroQuoteWithoutCatalogCalls() {
        CartResponse response = CartPricing.of(emptyCart(), catalog);

        assertTrue(response.items().isEmpty());
        assertEquals(Money.copZero(), response.currentSubtotal());
        assertTrue(response.currentPricingComplete());
        verifyNoInteractions(catalog);
    }

    @Test
    void quotesSeveralLinesFromCurrentCatalogPrices() {
        when(catalog.getProduct(MILK_ID)).thenReturn(Optional.of(new ProductCatalogInfo(MILK_ID, CURRENT_MILK)));
        when(catalog.getProduct(BREAD_ID)).thenReturn(Optional.of(new ProductCatalogInfo(BREAD_ID, CURRENT_BREAD)));

        CartResponse response = CartPricing.of(cartWithMilkAndBread(), catalog);

        assertEquals(CURRENT_MILK, response.items().get(0).currentUnitPrice());
        assertEquals(Money.cop(new BigDecimal("24.00")), response.items().get(0).currentLineAmount());
        assertEquals(CURRENT_BREAD, response.items().get(1).currentUnitPrice());
        assertEquals(CURRENT_BREAD, response.items().get(1).currentLineAmount());
        assertEquals(Money.cop(new BigDecimal("27.00")), response.currentSubtotal());
        assertTrue(response.currentPricingComplete());
        assertEquals(HISTORICAL_MILK, response.items().get(0).priceAtAddition());
    }

    @Test
    void doesNotUsePriceAtAdditionWhenCurrentPriceDiffers() {
        when(catalog.getProduct(MILK_ID)).thenReturn(Optional.of(new ProductCatalogInfo(MILK_ID, CURRENT_MILK)));

        CartResponse response = CartPricing.of(cartWithMilk(), catalog);

        assertEquals(HISTORICAL_MILK, response.items().get(0).priceAtAddition());
        assertEquals(CURRENT_MILK, response.items().get(0).currentUnitPrice());
        assertEquals(Money.cop(new BigDecimal("24.00")), response.currentSubtotal());
        verify(catalog).getProduct(MILK_ID);
    }

    @Test
    void omitsOfficialTotalWhenAProductHasNoCurrentPrice() {
        when(catalog.getProduct(MILK_ID)).thenReturn(Optional.empty());

        CartResponse response = CartPricing.of(cartWithMilk(), catalog);

        assertNull(response.items().get(0).currentUnitPrice());
        assertNull(response.items().get(0).currentLineAmount());
        assertNull(response.currentSubtotal());
        assertFalse(response.currentPricingComplete());
    }

    private static Cart emptyCart() {
        return Cart.create(CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), NOW, NOW);
    }

    private static Cart cartWithMilk() {
        return emptyCart().addProduct(ITEM_ID, MILK_ID, 2, HISTORICAL_MILK, NOW);
    }

    private static Cart cartWithMilkAndBread() {
        return cartWithMilk().addProduct(UUID.fromString("99999999-9999-9999-9999-000000000002"), BREAD_ID, 1, CURRENT_BREAD, NOW);
    }
}
