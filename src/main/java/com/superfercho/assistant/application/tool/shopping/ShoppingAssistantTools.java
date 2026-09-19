package com.superfercho.assistant.application.tool.shopping;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.shopping.application.port.in.AddProductToCartUseCase;
import com.superfercho.shopping.application.port.in.AddProductToShoppingListUseCase;
import com.superfercho.shopping.application.port.in.AddShoppingListToCartUseCase;
import com.superfercho.shopping.application.port.in.ChangeCartItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ChangeShoppingListItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearCartUseCase;
import com.superfercho.shopping.application.port.in.ClearShoppingListUseCase;
import com.superfercho.shopping.application.port.in.CreateShoppingListUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.application.port.in.GetShoppingListUseCase;
import com.superfercho.shopping.application.port.in.ListShoppingListsUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromCartUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromShoppingListUseCase;
import com.superfercho.shopping.application.port.in.RenameShoppingListUseCase;
import java.util.List;

public final class ShoppingAssistantTools {

    private ShoppingAssistantTools() {}

    public static List<AssistantTool> cart(
            GetCartUseCase getCartUseCase,
            AddProductToCartUseCase addProductToCartUseCase,
            ChangeCartItemQuantityUseCase changeCartItemQuantityUseCase,
            RemoveProductFromCartUseCase removeProductFromCartUseCase,
            ClearCartUseCase clearCartUseCase) {
        return List.of(
                new GetCartTool(getCartUseCase),
                new AddCartItemTool(addProductToCartUseCase),
                new ChangeCartItemQuantityTool(changeCartItemQuantityUseCase),
                new RemoveCartItemTool(removeProductFromCartUseCase),
                new ClearCartTool(clearCartUseCase));
    }

    public static List<AssistantTool> lists(
            ListShoppingListsUseCase listShoppingListsUseCase,
            GetShoppingListUseCase getShoppingListUseCase,
            CreateShoppingListUseCase createShoppingListUseCase,
            RenameShoppingListUseCase renameShoppingListUseCase,
            AddProductToShoppingListUseCase addProductToShoppingListUseCase,
            ChangeShoppingListItemQuantityUseCase changeShoppingListItemQuantityUseCase,
            RemoveProductFromShoppingListUseCase removeProductFromShoppingListUseCase,
            ClearShoppingListUseCase clearShoppingListUseCase,
            AddShoppingListToCartUseCase addShoppingListToCartUseCase) {
        return List.of(
                new ListShoppingListsTool(listShoppingListsUseCase),
                new GetShoppingListTool(getShoppingListUseCase),
                new CreateShoppingListTool(createShoppingListUseCase),
                new RenameShoppingListTool(renameShoppingListUseCase),
                new AddShoppingListItemTool(addProductToShoppingListUseCase),
                new ChangeShoppingListItemQuantityTool(changeShoppingListItemQuantityUseCase),
                new RemoveShoppingListItemTool(removeProductFromShoppingListUseCase),
                new ClearShoppingListTool(clearShoppingListUseCase),
                new AddShoppingListToCartTool(addShoppingListToCartUseCase));
    }
}
