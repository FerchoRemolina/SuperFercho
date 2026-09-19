package com.superfercho.assistant.application.tool;

import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import com.superfercho.assistant.application.tool.catalog.GetProductTool;
import com.superfercho.assistant.application.tool.catalog.ListCategoriesTool;
import com.superfercho.assistant.application.tool.catalog.ListProductsTool;
import com.superfercho.assistant.application.tool.catalog.SearchProductsTool;
import com.superfercho.assistant.application.tool.identity.ListAddressesTool;
import com.superfercho.assistant.application.tool.knowledge.SearchKnowledgeTool;
import com.superfercho.assistant.application.tool.orders.CancelOrderTool;
import com.superfercho.assistant.application.tool.orders.CheckoutTool;
import com.superfercho.assistant.application.tool.orders.OrdersAssistantTools;
import com.superfercho.assistant.application.tool.shopping.ShoppingAssistantTools;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.application.usecase.ListCategoriesUseCase;
import com.superfercho.catalog.application.usecase.ListProductsUseCase;
import com.superfercho.catalog.application.usecase.SearchProductsUseCase;
import com.superfercho.identity.application.usecase.ListAddressesUseCase;
import com.superfercho.knowledge.application.usecase.SearchKnowledgeUseCase;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.application.usecase.ListOrdersUseCase;
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
import java.util.ArrayList;
import java.util.List;

public final class AssistantToolAllowlist {

    private AssistantToolAllowlist() {}

    public static ToolRegistry create(
            SearchProductsUseCase searchProductsUseCase,
            GetProductUseCase getProductUseCase,
            ListProductsUseCase listProductsUseCase,
            ListCategoriesUseCase listCategoriesUseCase,
            GetCartUseCase getCartUseCase,
            AddProductToCartUseCase addProductToCartUseCase,
            ChangeCartItemQuantityUseCase changeCartItemQuantityUseCase,
            RemoveProductFromCartUseCase removeProductFromCartUseCase,
            ClearCartUseCase clearCartUseCase,
            ListShoppingListsUseCase listShoppingListsUseCase,
            GetShoppingListUseCase getShoppingListUseCase,
            CreateShoppingListUseCase createShoppingListUseCase,
            RenameShoppingListUseCase renameShoppingListUseCase,
            AddProductToShoppingListUseCase addProductToShoppingListUseCase,
            ChangeShoppingListItemQuantityUseCase changeShoppingListItemQuantityUseCase,
            RemoveProductFromShoppingListUseCase removeProductFromShoppingListUseCase,
            ClearShoppingListUseCase clearShoppingListUseCase,
            AddShoppingListToCartUseCase addShoppingListToCartUseCase,
            ListOrdersUseCase listOrdersUseCase,
            GetOrderUseCase getOrderUseCase,
            CurrentUserProvider currentUserProvider,
            PendingSensitiveActionStore pendingSensitiveActionStore,
            ListAddressesUseCase listAddressesUseCase,
            SearchKnowledgeUseCase searchKnowledgeUseCase) {
        List<AssistantTool> tools = new ArrayList<>();
        tools.add(new SearchProductsTool(searchProductsUseCase));
        tools.add(new GetProductTool(getProductUseCase));
        tools.add(new ListProductsTool(listProductsUseCase));
        tools.add(new ListCategoriesTool(listCategoriesUseCase));
        tools.addAll(ShoppingAssistantTools.cart(
                getCartUseCase,
                addProductToCartUseCase,
                changeCartItemQuantityUseCase,
                removeProductFromCartUseCase,
                clearCartUseCase));
        tools.addAll(ShoppingAssistantTools.lists(
                listShoppingListsUseCase,
                getShoppingListUseCase,
                createShoppingListUseCase,
                renameShoppingListUseCase,
                addProductToShoppingListUseCase,
                changeShoppingListItemQuantityUseCase,
                removeProductFromShoppingListUseCase,
                clearShoppingListUseCase,
                addShoppingListToCartUseCase));
        tools.addAll(OrdersAssistantTools.query(listOrdersUseCase, getOrderUseCase));
        tools.add(new CancelOrderTool(currentUserProvider, pendingSensitiveActionStore));
        tools.add(new CheckoutTool(
                currentUserProvider, getCartUseCase, getProductUseCase, pendingSensitiveActionStore));
        tools.add(new ListAddressesTool(listAddressesUseCase));
        tools.add(new SearchKnowledgeTool(searchKnowledgeUseCase));
        return new ToolRegistry(tools);
    }
}
