package com.superfercho.assistant.application.tool.shopping;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolParameter;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.shopping.application.dto.cart.AddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.ChangeCartItemQuantityCommand;
import com.superfercho.shopping.application.dto.cart.ClearCartCommand;
import com.superfercho.shopping.application.dto.cart.RemoveProductFromCartCommand;
import com.superfercho.shopping.application.port.in.AddProductToCartUseCase;
import com.superfercho.shopping.application.port.in.ChangeCartItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearCartUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromCartUseCase;

public final class GetCartTool implements AssistantTool {

    private final GetCartUseCase getCartUseCase;

    public GetCartTool(GetCartUseCase getCartUseCase) {
        this.getCartUseCase = getCartUseCase;
    }

    @Override
    public String name() {
        return ToolNames.GET_CART;
    }

    @Override
    public String description() {
        return "Get the authenticated customer's current cart.";
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
        return ToolResult.success(String.valueOf(getCartUseCase.execute()));
    }
}

final class AddCartItemTool implements AssistantTool {

    private final AddProductToCartUseCase addProductToCartUseCase;

    AddCartItemTool(AddProductToCartUseCase addProductToCartUseCase) {
        this.addProductToCartUseCase = addProductToCartUseCase;
    }

    @Override
    public String name() {
        return ToolNames.ADD_CART_ITEM;
    }

    @Override
    public String description() {
        return "Add a product to the authenticated customer's cart.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(
                new ToolParameter("productId", "uuid", true, "Product id"),
                new ToolParameter("quantity", "integer", true, "Quantity greater than 0"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(addProductToCartUseCase.execute(
                new AddProductToCartCommand(arguments.requireUuid("productId"), arguments.requirePositiveInt("quantity")))));
    }
}

final class ChangeCartItemQuantityTool implements AssistantTool {

    private final ChangeCartItemQuantityUseCase changeCartItemQuantityUseCase;

    ChangeCartItemQuantityTool(ChangeCartItemQuantityUseCase changeCartItemQuantityUseCase) {
        this.changeCartItemQuantityUseCase = changeCartItemQuantityUseCase;
    }

    @Override
    public String name() {
        return ToolNames.CHANGE_CART_ITEM_QUANTITY;
    }

    @Override
    public String description() {
        return "Change the quantity of a product in the authenticated customer's cart.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(
                new ToolParameter("productId", "uuid", true, "Product id"),
                new ToolParameter("quantity", "integer", true, "Quantity greater than 0"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(changeCartItemQuantityUseCase.execute(
                new ChangeCartItemQuantityCommand(
                        arguments.requireUuid("productId"), arguments.requirePositiveInt("quantity")))));
    }
}

final class RemoveCartItemTool implements AssistantTool {

    private final RemoveProductFromCartUseCase removeProductFromCartUseCase;

    RemoveCartItemTool(RemoveProductFromCartUseCase removeProductFromCartUseCase) {
        this.removeProductFromCartUseCase = removeProductFromCartUseCase;
    }

    @Override
    public String name() {
        return ToolNames.REMOVE_CART_ITEM;
    }

    @Override
    public String description() {
        return "Remove a product from the authenticated customer's cart.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(new ToolParameter("productId", "uuid", true, "Product id"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(
                removeProductFromCartUseCase.execute(new RemoveProductFromCartCommand(arguments.requireUuid("productId")))));
    }
}

final class ClearCartTool implements AssistantTool {

    private final ClearCartUseCase clearCartUseCase;

    ClearCartTool(ClearCartUseCase clearCartUseCase) {
        this.clearCartUseCase = clearCartUseCase;
    }

    @Override
    public String name() {
        return ToolNames.CLEAR_CART;
    }

    @Override
    public String description() {
        return "Clear all items from the authenticated customer's cart.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of();
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW_RISK;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(clearCartUseCase.execute(new ClearCartCommand())));
    }
}
