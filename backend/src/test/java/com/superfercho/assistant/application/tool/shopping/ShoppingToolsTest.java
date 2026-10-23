package com.superfercho.assistant.application.tool.shopping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.exception.InvalidToolArgumentsException;
import com.superfercho.assistant.application.packagesel.PackageSelectionDurations;
import com.superfercho.assistant.application.packagesel.PackageSelectionOption;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposal;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposalStatus;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.infrastructure.packagesel.InMemoryPackageSelectionProposalStore;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.shopping.application.dto.cart.AddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.shoppinglist.AddShoppingListToCartCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ChangeShoppingListItemQuantityCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListItemResponse;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;
import com.superfercho.shopping.application.port.in.AddProductToCartUseCase;
import com.superfercho.shopping.application.port.in.AddShoppingListToCartUseCase;
import com.superfercho.shopping.application.port.in.ChangeShoppingListItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.domain.exception.InvalidShoppingListItemException;
import com.superfercho.shopping.domain.model.CartStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingToolsTest {

    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID LIST_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-04-01T10:00:00Z");

    @Mock
    private GetCartUseCase getCartUseCase;

    @Mock
    private AddProductToCartUseCase addProductToCartUseCase;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ClockPort clockPort;

    @Mock
    private AddShoppingListToCartUseCase addShoppingListToCartUseCase;

    @Mock
    private ChangeShoppingListItemQuantityUseCase changeShoppingListItemQuantityUseCase;

    @Test
    void getCartDelegatesToUseCase() {
        when(getCartUseCase.execute()).thenReturn(cart());

        var result = new GetCartTool(getCartUseCase).execute(ToolArguments.of(Map.of()));
        assertTrue(result.success());
        assertTrue(result.content().contains("Informative quote"));
        assertTrue(result.content().contains("currentPricingComplete=true"));
        verify(getCartUseCase).execute();
    }

    @Test
    void getCartFailsWhenUseCaseReturnsNull() {
        when(getCartUseCase.execute()).thenReturn(null);

        var result = new GetCartTool(getCartUseCase).execute(ToolArguments.of(Map.of()));

        assertFalse(result.success());
        assertTrue(result.content().contains("returned no result"));
        assertFalse(result.content().contains("Informative quote"));
        assertFalse(result.content().contains("cart="));
        verify(getCartUseCase).execute();
    }

    @Test
    void getCartDoesNotClaimATotalWhenPricingIsIncomplete() {
        when(getCartUseCase.execute())
                .thenReturn(new CartResponse(
                        CART_ID,
                        CUSTOMER_ID,
                        CartStatus.ACTIVE,
                        List.of(),
                        NOW,
                        NOW,
                        null,
                        false));

        var result = new GetCartTool(getCartUseCase).execute(ToolArguments.of(Map.of()));
        assertTrue(result.success());
        assertTrue(result.content().contains("Official cart total is unavailable"));
        assertFalse(result.content().contains("Informative quote"));
    }

    @Test
    void addCartItemDelegatesToUseCase() {
        when(addProductToCartUseCase.execute(new AddProductToCartCommand(PRODUCT_ID, 2))).thenReturn(cart());

        assertTrue(new AddCartItemTool(
                        addProductToCartUseCase,
                        currentUserProvider,
                        clockPort,
                        new InMemoryPackageSelectionProposalStore())
                .execute(ToolArguments.of(Map.of("productId", PRODUCT_ID.toString(), "quantity", 2)))
                .success());
        verify(addProductToCartUseCase).execute(new AddProductToCartCommand(PRODUCT_ID, 2));
    }

    @Test
    void addShoppingListToCartUpdatesCartImmediatelyWithoutConfirmation() {
        when(addShoppingListToCartUseCase.execute(new AddShoppingListToCartCommand(LIST_ID))).thenReturn(cart());
        AddShoppingListToCartTool tool = new AddShoppingListToCartTool(addShoppingListToCartUseCase);

        ToolResult result = tool.execute(ToolArguments.of(Map.of("shoppingListId", LIST_ID.toString())));

        assertEquals(ToolRisk.LOW_RISK, tool.risk());
        assertTrue(result.success());
        assertFalse(result.awaitsConfirmation());
        assertTrue(result.content().contains("Cart updated immediately"));
        assertTrue(result.content().contains("not a placed order"));
        assertTrue(result.content().contains("quantity 0"));
        verify(addShoppingListToCartUseCase).execute(new AddShoppingListToCartCommand(LIST_ID));
    }

    @Test
    void addShoppingListToCartDoesNotClaimSuccessWhenUseCaseFails() {
        when(addShoppingListToCartUseCase.execute(new AddShoppingListToCartCommand(LIST_ID)))
                .thenThrow(new IllegalStateException("shopping list not found"));
        ToolRegistry registry = new ToolRegistry(List.of(new AddShoppingListToCartTool(addShoppingListToCartUseCase)));

        ToolResult result = registry.execute("add_shopping_list_to_cart", Map.of("shoppingListId", LIST_ID.toString()));

        assertFalse(result.success());
        assertFalse(result.awaitsConfirmation());
        assertEquals("shopping list not found", result.content());
    }

    @Test
    void changeShoppingListItemQuantityAllowsZeroAndPersistsThroughUseCase() {
        when(changeShoppingListItemQuantityUseCase.execute(
                        new ChangeShoppingListItemQuantityCommand(LIST_ID, PRODUCT_ID, 0)))
                .thenReturn(listWithZeroQuantity());

        ToolResult result = new ChangeShoppingListItemQuantityTool(changeShoppingListItemQuantityUseCase)
                .execute(ToolArguments.of(Map.of(
                        "shoppingListId", LIST_ID.toString(),
                        "productId", PRODUCT_ID.toString(),
                        "quantity", 0)));

        assertTrue(result.success());
        assertTrue(result.content().contains("quantity=0"));
        verify(changeShoppingListItemQuantityUseCase)
                .execute(new ChangeShoppingListItemQuantityCommand(LIST_ID, PRODUCT_ID, 0));
    }

    @Test
    void changeShoppingListItemQuantityDoesNotClaimSuccessWhenUseCaseRejects() {
        when(changeShoppingListItemQuantityUseCase.execute(
                        new ChangeShoppingListItemQuantityCommand(LIST_ID, PRODUCT_ID, 0)))
                .thenThrow(new InvalidShoppingListItemException("quantity must be greater than or equal to 0"));
        ToolRegistry registry =
                new ToolRegistry(List.of(new ChangeShoppingListItemQuantityTool(changeShoppingListItemQuantityUseCase)));

        ToolResult result = registry.execute(
                "change_shopping_list_item_quantity",
                Map.of(
                        "shoppingListId", LIST_ID.toString(),
                        "productId", PRODUCT_ID.toString(),
                        "quantity", 0));

        assertFalse(result.success());
        verify(changeShoppingListItemQuantityUseCase)
                .execute(new ChangeShoppingListItemQuantityCommand(LIST_ID, PRODUCT_ID, 0));
    }

    @Test
    void changeShoppingListItemQuantityRejectsNegativeWithoutCallingUseCase() {
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> new ChangeShoppingListItemQuantityTool(changeShoppingListItemQuantityUseCase)
                        .execute(ToolArguments.of(Map.of(
                                "shoppingListId", LIST_ID.toString(),
                                "productId", PRODUCT_ID.toString(),
                                "quantity", -1))));
        verify(changeShoppingListItemQuantityUseCase, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void addCartItemBlocksOfferedSkuAndAllowsOrdinaryAndOtherSkuAdds() {
        InMemoryPackageSelectionProposalStore store = new InMemoryPackageSelectionProposalStore();
        UUID otherProduct = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        store.replaceOffered(
                new PackageSelectionProposal(
                        UUID.randomUUID(),
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        "Arroz",
                        new BigDecimal("1.000"),
                        PresentationUnit.KG,
                        List.of(new PackageSelectionOption(2, new BigDecimal("2000.000"), PresentationUnit.G)),
                        new BigDecimal("2.000"),
                        PresentationUnit.KG,
                        PackageSelectionProposalStatus.OFFERED,
                        NOW,
                        NOW.plus(PackageSelectionDurations.PROPOSAL_TTL),
                        null),
                NOW);
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(addProductToCartUseCase.execute(new AddProductToCartCommand(otherProduct, 1))).thenReturn(cart());
        AddCartItemTool tool = new AddCartItemTool(addProductToCartUseCase, currentUserProvider, clockPort, store);

        ToolResult blocked = tool.execute(
                ToolArguments.of(Map.of("productId", PRODUCT_ID.toString(), "quantity", 2)));
        assertFalse(blocked.success());
        assertTrue(blocked.content().contains("active package selection"));
        verify(addProductToCartUseCase, never()).execute(new AddProductToCartCommand(PRODUCT_ID, 2));

        assertTrue(tool.execute(
                        ToolArguments.of(Map.of("productId", otherProduct.toString(), "quantity", 1)))
                .success());
        verify(addProductToCartUseCase).execute(new AddProductToCartCommand(otherProduct, 1));
    }

    @Test
    void addCartItemStillRejectsZeroQuantity() {
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> new AddCartItemTool(
                                addProductToCartUseCase,
                                currentUserProvider,
                                clockPort,
                                new InMemoryPackageSelectionProposalStore())
                        .execute(ToolArguments.of(Map.of("productId", PRODUCT_ID.toString(), "quantity", 0))));
        verify(addProductToCartUseCase, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    private static CartResponse cart() {
        return CartResponse.of(CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), NOW, NOW);
    }

    private static ShoppingListResponse listWithZeroQuantity() {
        return new ShoppingListResponse(
                LIST_ID,
                CUSTOMER_ID,
                "Mercado",
                List.of(new ShoppingListItemResponse(
                        UUID.fromString("99999999-9999-9999-9999-000000000001"), PRODUCT_ID, 0, NOW)),
                NOW,
                NOW);
    }
}
