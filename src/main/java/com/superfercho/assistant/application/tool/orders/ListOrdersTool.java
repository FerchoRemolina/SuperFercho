package com.superfercho.assistant.application.tool.orders;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolParameter;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.ListOrdersCommand;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.application.usecase.ListOrdersUseCase;

public final class ListOrdersTool implements AssistantTool {

    private final ListOrdersUseCase listOrdersUseCase;

    public ListOrdersTool(ListOrdersUseCase listOrdersUseCase) {
        this.listOrdersUseCase = listOrdersUseCase;
    }

    @Override
    public String name() {
        return ToolNames.LIST_ORDERS;
    }

    @Override
    public String description() {
        return "List orders of the authenticated customer.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(
                new ToolParameter("page", "integer", false, "Optional page number"),
                new ToolParameter("size", "integer", false, "Optional page size"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.QUERY;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        Integer page = arguments.optionalInt("page").orElse(null);
        Integer size = arguments.optionalPositiveInt("size").orElse(null);
        return ToolResult.success(String.valueOf(listOrdersUseCase.execute(new ListOrdersCommand(page, size))));
    }
}

final class GetOrderTool implements AssistantTool {

    private final GetOrderUseCase getOrderUseCase;

    GetOrderTool(GetOrderUseCase getOrderUseCase) {
        this.getOrderUseCase = getOrderUseCase;
    }

    @Override
    public String name() {
        return ToolNames.GET_ORDER;
    }

    @Override
    public String description() {
        return "Get one order owned by the authenticated customer.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(new ToolParameter("orderId", "uuid", true, "Order id"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.QUERY;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(
                String.valueOf(getOrderUseCase.execute(new GetOrderCommand(arguments.requireUuid("orderId")))));
    }
}
