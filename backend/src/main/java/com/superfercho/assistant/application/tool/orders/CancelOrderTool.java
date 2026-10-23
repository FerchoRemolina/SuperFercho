package com.superfercho.assistant.application.tool.orders;

import com.superfercho.assistant.application.confirmation.ConfirmationFingerprints;
import com.superfercho.assistant.application.confirmation.PendingSensitiveAction;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolParameter;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.exception.OrderOwnershipException;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.domain.model.Order;
import java.util.UUID;

public final class CancelOrderTool implements AssistantTool {

    private final CurrentUserProvider currentUserProvider;
    private final GetOrderUseCase getOrderUseCase;
    private final ClockPort clockPort;
    private final PendingSensitiveActionStore pendingSensitiveActionStore;

    public CancelOrderTool(
            CurrentUserProvider currentUserProvider,
            GetOrderUseCase getOrderUseCase,
            ClockPort clockPort,
            PendingSensitiveActionStore pendingSensitiveActionStore) {
        this.currentUserProvider = currentUserProvider;
        this.getOrderUseCase = getOrderUseCase;
        this.clockPort = clockPort;
        this.pendingSensitiveActionStore = pendingSensitiveActionStore;
    }

    @Override
    public String name() {
        return ToolNames.CANCEL_ORDER;
    }

    @Override
    public String description() {
        return "Prepare cancellation of an order owned by the authenticated customer. Requires explicit user confirmation.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(new ToolParameter("orderId", "uuid", true, "Order id"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.SENSITIVE;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        UUID userId = currentUserProvider.getCurrentUserId();
        UUID orderId = arguments.requireUuid("orderId");
        OrderResult order;
        try {
            order = getOrderUseCase.execute(new GetOrderCommand(orderId));
        } catch (OrderNotFoundException | OrderOwnershipException exception) {
            return ToolResult.failure("order not found");
        }
        if (!Order.isCustomerCancellable(order.status(), order.confirmedAt(), clockPort.currentTime())) {
            return ToolResult.failure("order cannot be cancelled in its current state");
        }
        String token = UUID.randomUUID().toString();
        pendingSensitiveActionStore.deleteByUserId(userId);
        pendingSensitiveActionStore.save(PendingSensitiveAction.cancelOrder(
                token, userId, ConfirmationFingerprints.cancelOrder(orderId), orderId));
        return ToolResult.confirmationRequired(
                "Cancellation prepared for order " + orderId + ". Explicit confirmation is required.", token);
    }
}
