package com.superfercho.assistant.application.tool.orders;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.application.usecase.ListOrdersUseCase;
import java.util.List;

public final class OrdersAssistantTools {

    private OrdersAssistantTools() {}

    public static List<AssistantTool> query(ListOrdersUseCase listOrdersUseCase, GetOrderUseCase getOrderUseCase) {
        return List.of(new ListOrdersTool(listOrdersUseCase), new GetOrderTool(getOrderUseCase));
    }
}
