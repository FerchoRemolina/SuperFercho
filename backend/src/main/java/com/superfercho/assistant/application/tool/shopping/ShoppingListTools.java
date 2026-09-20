package com.superfercho.assistant.application.tool.shopping;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolParameter;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.AddShoppingListToCartCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ChangeShoppingListItemQuantityCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ClearShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.CreateShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.GetShoppingListQuery;
import com.superfercho.shopping.application.dto.shoppinglist.RemoveProductFromShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.RenameShoppingListCommand;
import com.superfercho.shopping.application.port.in.AddProductToShoppingListUseCase;
import com.superfercho.shopping.application.port.in.AddShoppingListToCartUseCase;
import com.superfercho.shopping.application.port.in.ChangeShoppingListItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearShoppingListUseCase;
import com.superfercho.shopping.application.port.in.CreateShoppingListUseCase;
import com.superfercho.shopping.application.port.in.GetShoppingListUseCase;
import com.superfercho.shopping.application.port.in.ListShoppingListsUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromShoppingListUseCase;
import com.superfercho.shopping.application.port.in.RenameShoppingListUseCase;

final class ListShoppingListsTool implements AssistantTool {

    private final ListShoppingListsUseCase listShoppingListsUseCase;

    ListShoppingListsTool(ListShoppingListsUseCase listShoppingListsUseCase) {
        this.listShoppingListsUseCase = listShoppingListsUseCase;
    }

    @Override
    public String name() {
        return ToolNames.LIST_SHOPPING_LISTS;
    }

    @Override
    public String description() {
        return "List shopping lists of the authenticated customer.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of();
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.QUERY;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(listShoppingListsUseCase.execute()));
    }
}

final class GetShoppingListTool implements AssistantTool {

    private final GetShoppingListUseCase getShoppingListUseCase;

    GetShoppingListTool(GetShoppingListUseCase getShoppingListUseCase) {
        this.getShoppingListUseCase = getShoppingListUseCase;
    }

    @Override
    public String name() {
        return ToolNames.GET_SHOPPING_LIST;
    }

    @Override
    public String description() {
        return "Get one shopping list owned by the authenticated customer.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(new ToolParameter("shoppingListId", "uuid", true, "Shopping list id"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.QUERY;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(
                getShoppingListUseCase.execute(new GetShoppingListQuery(arguments.requireUuid("shoppingListId")))));
    }
}

final class CreateShoppingListTool implements AssistantTool {

    private final CreateShoppingListUseCase createShoppingListUseCase;

    CreateShoppingListTool(CreateShoppingListUseCase createShoppingListUseCase) {
        this.createShoppingListUseCase = createShoppingListUseCase;
    }

    @Override
    public String name() {
        return ToolNames.CREATE_SHOPPING_LIST;
    }

    @Override
    public String description() {
        return "Create a shopping list for the authenticated customer.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(new ToolParameter("name", "string", true, "List name"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(
                createShoppingListUseCase.execute(new CreateShoppingListCommand(arguments.requireText("name")))));
    }
}

final class RenameShoppingListTool implements AssistantTool {

    private final RenameShoppingListUseCase renameShoppingListUseCase;

    RenameShoppingListTool(RenameShoppingListUseCase renameShoppingListUseCase) {
        this.renameShoppingListUseCase = renameShoppingListUseCase;
    }

    @Override
    public String name() {
        return ToolNames.RENAME_SHOPPING_LIST;
    }

    @Override
    public String description() {
        return "Rename a shopping list owned by the authenticated customer.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(
                new ToolParameter("shoppingListId", "uuid", true, "Shopping list id"),
                new ToolParameter("name", "string", true, "New name"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(renameShoppingListUseCase.execute(
                new RenameShoppingListCommand(arguments.requireUuid("shoppingListId"), arguments.requireText("name")))));
    }
}

final class AddShoppingListItemTool implements AssistantTool {

    private final AddProductToShoppingListUseCase addProductToShoppingListUseCase;

    AddShoppingListItemTool(AddProductToShoppingListUseCase addProductToShoppingListUseCase) {
        this.addProductToShoppingListUseCase = addProductToShoppingListUseCase;
    }

    @Override
    public String name() {
        return ToolNames.ADD_SHOPPING_LIST_ITEM;
    }

    @Override
    public String description() {
        return "Add a product to a shopping list owned by the authenticated customer.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(
                new ToolParameter("shoppingListId", "uuid", true, "Shopping list id"),
                new ToolParameter("productId", "uuid", true, "Product id"),
                new ToolParameter("quantity", "integer", true, "Quantity greater than 0"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(addProductToShoppingListUseCase.execute(
                new AddProductToShoppingListCommand(
                        arguments.requireUuid("shoppingListId"),
                        arguments.requireUuid("productId"),
                        arguments.requirePositiveInt("quantity")))));
    }
}

final class ChangeShoppingListItemQuantityTool implements AssistantTool {

    private final ChangeShoppingListItemQuantityUseCase changeShoppingListItemQuantityUseCase;

    ChangeShoppingListItemQuantityTool(
            ChangeShoppingListItemQuantityUseCase changeShoppingListItemQuantityUseCase) {
        this.changeShoppingListItemQuantityUseCase = changeShoppingListItemQuantityUseCase;
    }

    @Override
    public String name() {
        return ToolNames.CHANGE_SHOPPING_LIST_ITEM_QUANTITY;
    }

    @Override
    public String description() {
        return "Change a product quantity on a shopping list owned by the authenticated customer.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(
                new ToolParameter("shoppingListId", "uuid", true, "Shopping list id"),
                new ToolParameter("productId", "uuid", true, "Product id"),
                new ToolParameter("quantity", "integer", true, "Quantity greater than 0"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(changeShoppingListItemQuantityUseCase.execute(
                new ChangeShoppingListItemQuantityCommand(
                        arguments.requireUuid("shoppingListId"),
                        arguments.requireUuid("productId"),
                        arguments.requirePositiveInt("quantity")))));
    }
}

final class RemoveShoppingListItemTool implements AssistantTool {

    private final RemoveProductFromShoppingListUseCase removeProductFromShoppingListUseCase;

    RemoveShoppingListItemTool(RemoveProductFromShoppingListUseCase removeProductFromShoppingListUseCase) {
        this.removeProductFromShoppingListUseCase = removeProductFromShoppingListUseCase;
    }

    @Override
    public String name() {
        return ToolNames.REMOVE_SHOPPING_LIST_ITEM;
    }

    @Override
    public String description() {
        return "Remove a product from a shopping list owned by the authenticated customer.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(
                new ToolParameter("shoppingListId", "uuid", true, "Shopping list id"),
                new ToolParameter("productId", "uuid", true, "Product id"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(removeProductFromShoppingListUseCase.execute(
                new RemoveProductFromShoppingListCommand(
                        arguments.requireUuid("shoppingListId"), arguments.requireUuid("productId")))));
    }
}

final class ClearShoppingListTool implements AssistantTool {

    private final ClearShoppingListUseCase clearShoppingListUseCase;

    ClearShoppingListTool(ClearShoppingListUseCase clearShoppingListUseCase) {
        this.clearShoppingListUseCase = clearShoppingListUseCase;
    }

    @Override
    public String name() {
        return ToolNames.CLEAR_SHOPPING_LIST;
    }

    @Override
    public String description() {
        return "Clear all items from a shopping list owned by the authenticated customer.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(new ToolParameter("shoppingListId", "uuid", true, "Shopping list id"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(clearShoppingListUseCase.execute(
                new ClearShoppingListCommand(arguments.requireUuid("shoppingListId")))));
    }
}

final class AddShoppingListToCartTool implements AssistantTool {

    private final AddShoppingListToCartUseCase addShoppingListToCartUseCase;

    AddShoppingListToCartTool(AddShoppingListToCartUseCase addShoppingListToCartUseCase) {
        this.addShoppingListToCartUseCase = addShoppingListToCartUseCase;
    }

    @Override
    public String name() {
        return ToolNames.ADD_SHOPPING_LIST_TO_CART;
    }

    @Override
    public String description() {
        return "Add every product from a shopping list into the authenticated customer's cart.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(new ToolParameter("shoppingListId", "uuid", true, "Shopping list id"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(addShoppingListToCartUseCase.execute(
                new AddShoppingListToCartCommand(arguments.requireUuid("shoppingListId")))));
    }
}
