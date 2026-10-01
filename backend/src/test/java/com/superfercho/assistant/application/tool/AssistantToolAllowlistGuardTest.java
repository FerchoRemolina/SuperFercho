package com.superfercho.assistant.application.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
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
import java.util.List;
import org.junit.jupiter.api.Test;

class AssistantToolAllowlistGuardTest {

    private static final int EXPECTED_TOOL_COUNT = 24;

    private static final List<String> EXPECTED_TOOL_NAMES = List.of(
            "search_products",
            "get_product",
            "list_products",
            "list_categories",
            "get_cart",
            "add_cart_item",
            "change_cart_item_quantity",
            "remove_cart_item",
            "clear_cart",
            "list_shopping_lists",
            "get_shopping_list",
            "create_shopping_list",
            "rename_shopping_list",
            "add_shopping_list_item",
            "change_shopping_list_item_quantity",
            "remove_shopping_list_item",
            "clear_shopping_list",
            "add_shopping_list_to_cart",
            "list_orders",
            "get_order",
            "cancel_order",
            "checkout",
            "list_addresses",
            "search_knowledge");

    @Test
    void registeredToolsMatchTheExpectedContract() {
        ToolRegistry registry = AssistantToolAllowlist.create(
                mock(SearchProductsUseCase.class),
                mock(GetProductUseCase.class),
                mock(ListProductsUseCase.class),
                mock(ListCategoriesUseCase.class),
                mock(GetCartUseCase.class),
                mock(AddProductToCartUseCase.class),
                mock(ChangeCartItemQuantityUseCase.class),
                mock(RemoveProductFromCartUseCase.class),
                mock(ClearCartUseCase.class),
                mock(ListShoppingListsUseCase.class),
                mock(GetShoppingListUseCase.class),
                mock(CreateShoppingListUseCase.class),
                mock(RenameShoppingListUseCase.class),
                mock(AddProductToShoppingListUseCase.class),
                mock(ChangeShoppingListItemQuantityUseCase.class),
                mock(RemoveProductFromShoppingListUseCase.class),
                mock(ClearShoppingListUseCase.class),
                mock(AddShoppingListToCartUseCase.class),
                mock(ListOrdersUseCase.class),
                mock(GetOrderUseCase.class),
                mock(CurrentUserProvider.class),
                mock(PendingSensitiveActionStore.class),
                mock(ListAddressesUseCase.class),
                mock(SearchKnowledgeUseCase.class));

        List<String> names = registry.allowlist().stream().map(AssistantTool::name).toList();

        assertThat(names)
                .hasSize(EXPECTED_TOOL_COUNT)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(EXPECTED_TOOL_NAMES);
    }
}
