package com.superfercho.shopping.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import com.superfercho.shopping.application.dto.favorite.AddFavoriteCommand;
import com.superfercho.shopping.application.dto.favorite.AddFavoriteResult;
import com.superfercho.shopping.application.dto.favorite.FavoriteItemResponse;
import com.superfercho.shopping.application.dto.favorite.FavoriteListResult;
import com.superfercho.shopping.application.dto.favorite.FavoriteProductView;
import com.superfercho.shopping.application.dto.favorite.RemoveFavoriteCommand;
import com.superfercho.shopping.application.exception.DuplicateFavoriteException;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.application.port.out.ProductCardCatalogPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.domain.model.Favorite;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FavoriteApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:05:00Z");
    private static final Instant EARLIER = Instant.parse("2026-09-21T09:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SECOND_PRODUCT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CATEGORY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID FAVORITE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private FavoriteRepositoryPort favoriteRepository;

    @Mock
    private ProductCatalogPort productCatalogPort;

    @Mock
    private ProductCardCatalogPort productCardCatalogPort;

    @Mock
    private ClockPort clockPort;

    private FavoriteApplicationService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteApplicationService(
                currentUserProvider, favoriteRepository, productCatalogPort, productCardCatalogPort, clockPort);
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
    }

    @Test
    void shouldAddFavoriteWhenProductIsSellable() {
        when(productCatalogPort.getProduct(PRODUCT_ID)).thenReturn(Optional.of(catalogProduct()));
        when(favoriteRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
        when(clockPort.currentTime()).thenReturn(NOW);
        when(favoriteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddFavoriteResult result = favoriteService.execute(new AddFavoriteCommand(PRODUCT_ID));

        assertTrue(result.created());
        assertEquals(PRODUCT_ID, result.favorite().productId());
        assertEquals(NOW, result.favorite().createdAt());
        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository).save(captor.capture());
        assertEquals(CUSTOMER_ID, captor.getValue().customerId());
        assertEquals(PRODUCT_ID, captor.getValue().productId());
        verify(currentUserProvider).getCurrentUserId();
    }

    @Test
    void shouldRejectWhenProductDoesNotExist() {
        when(productCatalogPort.getProduct(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> favoriteService.execute(new AddFavoriteCommand(PRODUCT_ID)));

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenProductIsInactive() {
        when(productCatalogPort.getProduct(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> favoriteService.execute(new AddFavoriteCommand(PRODUCT_ID)));

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenCategoryIsInactive() {
        when(productCatalogPort.getProduct(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> favoriteService.execute(new AddFavoriteCommand(PRODUCT_ID)));

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void shouldAddFavoriteWhenStockIsZeroIfProductIsSellable() {
        when(productCatalogPort.getProduct(PRODUCT_ID)).thenReturn(Optional.of(catalogProduct()));
        when(favoriteRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
        when(clockPort.currentTime()).thenReturn(NOW);
        when(favoriteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddFavoriteResult result = favoriteService.execute(new AddFavoriteCommand(PRODUCT_ID));

        assertTrue(result.created());
        verify(favoriteRepository).save(any());
    }

    @Test
    void shouldReturnExistingFavoriteWithoutCreatingDuplicate() {
        Favorite existing = Favorite.create(FAVORITE_ID, CUSTOMER_ID, PRODUCT_ID, EARLIER);
        when(productCatalogPort.getProduct(PRODUCT_ID)).thenReturn(Optional.of(catalogProduct()));
        when(favoriteRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID)).thenReturn(Optional.of(existing));

        AddFavoriteResult result = favoriteService.execute(new AddFavoriteCommand(PRODUCT_ID));

        assertFalse(result.created());
        assertEquals(FAVORITE_ID, result.favorite().id());
        assertEquals(EARLIER, result.favorite().createdAt());
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void shouldReturnExistingFavoriteWhenConcurrentSaveHitsUniqueConstraint() {
        Favorite existing = Favorite.create(FAVORITE_ID, CUSTOMER_ID, PRODUCT_ID, EARLIER);
        when(productCatalogPort.getProduct(PRODUCT_ID)).thenReturn(Optional.of(catalogProduct()));
        when(favoriteRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(favoriteRepository.save(any())).thenThrow(new DuplicateFavoriteException());

        AddFavoriteResult result = favoriteService.execute(new AddFavoriteCommand(PRODUCT_ID));

        assertFalse(result.created());
        assertEquals(FAVORITE_ID, result.favorite().id());
        verify(favoriteRepository).save(any());
        verify(favoriteRepository, times(2)).findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID);
    }

    @Test
    void shouldRemoveExistingFavoriteForCurrentCustomer() {
        favoriteService.execute(new RemoveFavoriteCommand(PRODUCT_ID));

        verify(favoriteRepository).deleteByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID);
        verify(favoriteRepository, never()).deleteByCustomerIdAndProductId(eq(OTHER_CUSTOMER_ID), any());
        verify(currentUserProvider).getCurrentUserId();
    }

    @Test
    void shouldNoOpWhenRemovingMissingFavorite() {
        favoriteService.execute(new RemoveFavoriteCommand(PRODUCT_ID));

        verify(favoriteRepository).deleteByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID);
    }

    @Test
    void shouldListFavoritesInCreatedAtOrderUsingBatchCatalogLookup() {
        Favorite newer = Favorite.create(FAVORITE_ID, CUSTOMER_ID, PRODUCT_ID, NOW);
        Favorite older =
                Favorite.create(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), CUSTOMER_ID, SECOND_PRODUCT_ID, EARLIER);
        when(favoriteRepository.findAllByCustomerIdOrderedByCreatedAtDesc(CUSTOMER_ID))
                .thenReturn(List.of(newer, older));
        when(productCardCatalogPort.findCardsByIds(List.of(PRODUCT_ID, SECOND_PRODUCT_ID)))
                .thenReturn(List.of(inactiveCard(SECOND_PRODUCT_ID), activeCard(PRODUCT_ID)));

        FavoriteListResult result = favoriteService.execute();

        assertEquals(2, result.items().size());
        FavoriteItemResponse first = result.items().get(0);
        FavoriteItemResponse second = result.items().get(1);
        assertEquals(PRODUCT_ID, first.productId());
        assertEquals(NOW, first.createdAt());
        assertEquals("ACTIVE", first.product().status());
        assertTrue(first.product().available());
        assertEquals(SECOND_PRODUCT_ID, second.productId());
        assertEquals("INACTIVE", second.product().status());
        assertFalse(second.product().available());
        verify(productCardCatalogPort, times(1)).findCardsByIds(List.of(PRODUCT_ID, SECOND_PRODUCT_ID));
        verify(currentUserProvider).getCurrentUserId();
    }

    @Test
    void shouldLeaveProductNullWhenCatalogDoesNotReturnTheProduct() {
        Favorite favorite = Favorite.create(FAVORITE_ID, CUSTOMER_ID, PRODUCT_ID, NOW);
        when(favoriteRepository.findAllByCustomerIdOrderedByCreatedAtDesc(CUSTOMER_ID)).thenReturn(List.of(favorite));
        when(productCardCatalogPort.findCardsByIds(List.of(PRODUCT_ID))).thenReturn(List.of());

        FavoriteListResult result = favoriteService.execute();

        assertEquals(1, result.items().size());
        assertEquals(PRODUCT_ID, result.items().get(0).productId());
        assertNull(result.items().get(0).product());
        verify(favoriteRepository, never()).deleteByCustomerIdAndProductId(any(), any());
    }

    @Test
    void shouldSkipCatalogLookupWhenCustomerHasNoFavorites() {
        when(favoriteRepository.findAllByCustomerIdOrderedByCreatedAtDesc(CUSTOMER_ID)).thenReturn(List.of());

        FavoriteListResult result = favoriteService.execute();

        assertTrue(result.items().isEmpty());
        verify(productCardCatalogPort, never()).findCardsByIds(any());
    }

    private static ProductCatalogInfo catalogProduct() {
        return new ProductCatalogInfo(PRODUCT_ID, PRICE);
    }

    private static FavoriteProductView activeCard(UUID productId) {
        return new FavoriteProductView(
                productId, "Leche entera", "Alpina", PRICE, "https://img.test/milk.png", CATEGORY_ID, "ACTIVE", true);
    }

    private static FavoriteProductView inactiveCard(UUID productId) {
        return new FavoriteProductView(
                productId, "Pan", "Bimbo", PRICE, null, CATEGORY_ID, "INACTIVE", false);
    }
}
