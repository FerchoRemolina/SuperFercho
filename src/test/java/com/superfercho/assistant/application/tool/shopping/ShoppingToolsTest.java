package com.superfercho.assistant.application.tool.shopping;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.cart.AddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.shoppinglist.AddShoppingListToCartCommand;
import com.superfercho.shopping.application.port.in.AddProductToCartUseCase;
import com.superfercho.shopping.application.port.in.AddShoppingListToCartUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
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
    private AddShoppingListToCartUseCase addShoppingListToCartUseCase;

    @Test
    void getCartDelegatesToUseCase() {
        when(getCartUseCase.execute()).thenReturn(cart());

        assertTrue(new GetCartTool(getCartUseCase).execute(ToolArguments.of(Map.of())).success());
        verify(getCartUseCase).execute();
    }

    @Test
    void addCartItemDelegatesToUseCase() {
        when(addProductToCartUseCase.execute(new AddProductToCartCommand(PRODUCT_ID, 2))).thenReturn(cart());

        assertTrue(new AddCartItemTool(addProductToCartUseCase)
                .execute(ToolArguments.of(Map.of("productId", PRODUCT_ID.toString(), "quantity", 2)))
                .success());
        verify(addProductToCartUseCase).execute(new AddProductToCartCommand(PRODUCT_ID, 2));
    }

    @Test
    void addShoppingListToCartDelegatesToUseCase() {
        when(addShoppingListToCartUseCase.execute(new AddShoppingListToCartCommand(LIST_ID))).thenReturn(cart());

        assertTrue(new AddShoppingListToCartTool(addShoppingListToCartUseCase)
                .execute(ToolArguments.of(Map.of("shoppingListId", LIST_ID.toString())))
                .success());
        verify(addShoppingListToCartUseCase).execute(new AddShoppingListToCartCommand(LIST_ID));
    }

    private static CartResponse cart() {
        return new CartResponse(CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), NOW, NOW);
    }
}
