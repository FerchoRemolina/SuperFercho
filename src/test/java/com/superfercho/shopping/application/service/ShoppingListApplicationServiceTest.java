package com.superfercho.shopping.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ChangeShoppingListItemQuantityCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ClearShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.CreateShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.GetShoppingListQuery;
import com.superfercho.shopping.application.dto.shoppinglist.RemoveProductFromShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.RenameShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.exception.ShoppingListNotFoundException;
import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.domain.exception.InvalidShoppingListException;
import com.superfercho.shopping.domain.exception.InvalidShoppingListItemException;
import com.superfercho.shopping.domain.model.ShoppingList;
import com.superfercho.shopping.domain.model.ShoppingListItem;
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
class ShoppingListApplicationServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-04-01T10:05:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LIST_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SECOND_LIST_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID MILK_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BREAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final Money MILK_PRICE = Money.cop(new BigDecimal("10.50"));

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ShoppingListRepositoryPort shoppingListRepository;

    @Mock
    private ProductCatalogPort productCatalogPort;

    @Mock
    private ClockPort clockPort;

    private ShoppingListApplicationService shoppingListService;

    @BeforeEach
    void setUp() {
        shoppingListService = new ShoppingListApplicationService(
                currentUserProvider, shoppingListRepository, productCatalogPort, clockPort);
    }

    @Test
    void shouldCreateShoppingListForCurrentUserFromProvider() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingListResponse response = shoppingListService.execute(new CreateShoppingListCommand("Mercado semanal"));

        verify(currentUserProvider).getCurrentUserId();
        assertEquals(CUSTOMER_ID, response.customerId());
        assertEquals("Mercado semanal", response.name());
        assertTrue(response.items().isEmpty());
        assertEquals(NOW, response.createdAt());
        assertEquals(NOW, response.updatedAt());
        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void shouldGetShoppingList() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(listWithMilk()));

        ShoppingListResponse response = shoppingListService.execute(new GetShoppingListQuery(LIST_ID));

        assertEquals(LIST_ID, response.id());
        assertEquals("Mercado semanal", response.name());
        assertEquals(1, response.items().size());
        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void shouldListShoppingListsForCurrentUserFromProvider() {
        ShoppingList second = ShoppingList.create(
                SECOND_LIST_ID, CUSTOMER_ID, "Fin de semana", List.of(), CREATED_AT, CREATED_AT);
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findAllByCustomerId(CUSTOMER_ID)).thenReturn(List.of(emptyList(), second));

        List<ShoppingListResponse> response = shoppingListService.execute();

        verify(currentUserProvider).getCurrentUserId();
        verify(shoppingListRepository).findAllByCustomerId(CUSTOMER_ID);
        assertEquals(2, response.size());
        assertEquals(LIST_ID, response.get(0).id());
        assertEquals(SECOND_LIST_ID, response.get(1).id());
        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void shouldRenameShoppingList() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(emptyList()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingListResponse response =
                shoppingListService.execute(new RenameShoppingListCommand(LIST_ID, "Fin de semana"));

        assertEquals("Fin de semana", response.name());
        assertEquals(NOW, response.updatedAt());
        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void shouldAddNewProductToShoppingList() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(emptyList()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(productCatalogPort.getProduct(MILK_ID)).thenReturn(Optional.of(new ProductCatalogInfo(MILK_ID, MILK_PRICE)));
        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingListResponse response =
                shoppingListService.execute(new AddProductToShoppingListCommand(LIST_ID, MILK_ID, 2));

        assertEquals(1, response.items().size());
        assertEquals(MILK_ID, response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(NOW, response.updatedAt());
        verify(productCatalogPort).getProduct(MILK_ID);
        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void shouldIncrementQuantityWhenProductAlreadyInShoppingList() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(listWithMilk()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(productCatalogPort.getProduct(MILK_ID)).thenReturn(Optional.of(new ProductCatalogInfo(MILK_ID, MILK_PRICE)));
        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingListResponse response =
                shoppingListService.execute(new AddProductToShoppingListCommand(LIST_ID, MILK_ID, 3));

        assertEquals(1, response.items().size());
        assertEquals(ITEM_ID, response.items().get(0).id());
        assertEquals(5, response.items().get(0).quantity());
        verify(productCatalogPort).getProduct(MILK_ID);
        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void shouldChangeShoppingListItemQuantity() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(listWithMilk()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingListResponse response =
                shoppingListService.execute(new ChangeShoppingListItemQuantityCommand(LIST_ID, MILK_ID, 4));

        assertEquals(4, response.items().get(0).quantity());
        assertEquals(NOW, response.updatedAt());
        verify(shoppingListRepository).save(any(ShoppingList.class));
        verify(productCatalogPort, never()).getProduct(any());
    }

    @Test
    void shouldRemoveProductFromShoppingList() {
        ShoppingList list = emptyList()
                .addProduct(ITEM_ID, MILK_ID, 1, CREATED_AT)
                .addProduct(UUID.fromString("99999999-9999-9999-9999-000000000002"), BREAD_ID, 1, CREATED_AT);
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(list));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingListResponse response =
                shoppingListService.execute(new RemoveProductFromShoppingListCommand(LIST_ID, MILK_ID));

        assertEquals(1, response.items().size());
        assertEquals(BREAD_ID, response.items().get(0).productId());
        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void shouldClearShoppingList() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(listWithMilk()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingListResponse response = shoppingListService.execute(new ClearShoppingListCommand(LIST_ID));

        assertTrue(response.items().isEmpty());
        assertEquals(LIST_ID, response.id());
        assertEquals(NOW, response.updatedAt());
        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void shouldRejectGetWhenShoppingListDoesNotExist() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.empty());

        assertThrows(
                ShoppingListNotFoundException.class,
                () -> shoppingListService.execute(new GetShoppingListQuery(LIST_ID)));
        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void shouldRejectMutationWhenShoppingListDoesNotExist() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.empty());

        assertThrows(
                ShoppingListNotFoundException.class,
                () -> shoppingListService.execute(new RenameShoppingListCommand(LIST_ID, "Otro")));
        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenShoppingListBelongsToAnotherCustomer() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OTHER_CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(emptyList()));

        assertThrows(
                ShoppingListNotFoundException.class,
                () -> shoppingListService.execute(new GetShoppingListQuery(LIST_ID)));
        verify(currentUserProvider).getCurrentUserId();
        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void shouldRejectAddWhenProductDoesNotExist() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(productCatalogPort.getProduct(MILK_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> shoppingListService.execute(new AddProductToShoppingListCommand(LIST_ID, MILK_ID, 1)));
        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void shouldPropagateDomainExceptionWhenQuantityIsInvalid() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(listWithMilk()));
        when(clockPort.currentTime()).thenReturn(NOW);

        assertThrows(
                InvalidShoppingListItemException.class,
                () -> shoppingListService.execute(new ChangeShoppingListItemQuantityCommand(LIST_ID, MILK_ID, 0)));
        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void shouldPropagateDomainExceptionWhenNameIsBlank() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(emptyList()));
        when(clockPort.currentTime()).thenReturn(NOW);

        assertThrows(
                InvalidShoppingListException.class,
                () -> shoppingListService.execute(new RenameShoppingListCommand(LIST_ID, "  ")));
        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void shouldPropagateUnauthenticatedUserWhenCreatingShoppingList() {
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(
                UnauthenticatedUserException.class,
                () -> shoppingListService.execute(new CreateShoppingListCommand("Mercado semanal")));
        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void shouldPropagateUnauthenticatedUserWhenListingShoppingLists() {
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(UnauthenticatedUserException.class, shoppingListService::execute);
        verify(shoppingListRepository, never()).findAllByCustomerId(any());
    }

    @Test
    void shouldPropagateUnauthenticatedUserWhenGettingShoppingList() {
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(
                UnauthenticatedUserException.class,
                () -> shoppingListService.execute(new GetShoppingListQuery(LIST_ID)));
        verify(shoppingListRepository, never()).findById(any());
    }

    private ShoppingList emptyList() {
        return ShoppingList.create(LIST_ID, CUSTOMER_ID, "Mercado semanal", List.of(), CREATED_AT, CREATED_AT);
    }

    private ShoppingList listWithMilk() {
        return ShoppingList.create(
                LIST_ID,
                CUSTOMER_ID,
                "Mercado semanal",
                List.of(ShoppingListItem.create(ITEM_ID, MILK_ID, 2, CREATED_AT)),
                CREATED_AT,
                CREATED_AT);
    }
}
