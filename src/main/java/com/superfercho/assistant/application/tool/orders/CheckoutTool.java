package com.superfercho.assistant.application.tool.orders;

import com.superfercho.assistant.application.confirmation.ConfirmationFingerprints;
import com.superfercho.assistant.application.confirmation.PendingSensitiveAction;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolParameter;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutItem;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.shopping.application.dto.cart.CartItemResponse;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CheckoutTool implements AssistantTool {

    private final CurrentUserProvider currentUserProvider;
    private final GetCartUseCase getCartUseCase;
    private final GetProductUseCase getProductUseCase;
    private final PendingSensitiveActionStore pendingSensitiveActionStore;

    public CheckoutTool(
            CurrentUserProvider currentUserProvider,
            GetCartUseCase getCartUseCase,
            GetProductUseCase getProductUseCase,
            PendingSensitiveActionStore pendingSensitiveActionStore) {
        this.currentUserProvider = currentUserProvider;
        this.getCartUseCase = getCartUseCase;
        this.getProductUseCase = getProductUseCase;
        this.pendingSensitiveActionStore = pendingSensitiveActionStore;
    }

    @Override
    public String name() {
        return ToolNames.CHECKOUT;
    }

    @Override
    public String description() {
        return "Prepare checkout for the authenticated customer's cart. Requires explicit user confirmation.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(
                new ToolParameter("addressId", "uuid", true, "Shipping address id"),
                new ToolParameter("paymentMethod", "string", true, "SIMULATED_CARD or CASH_ON_DELIVERY"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.SENSITIVE;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        UUID userId = currentUserProvider.getCurrentUserId();
        CartResponse cart = getCartUseCase.execute();
        if (cart.items().isEmpty()) {
            return ToolResult.failure("cart is empty");
        }
        List<CheckoutItem> items = new ArrayList<>();
        for (CartItemResponse item : cart.items()) {
            ProductResult product = getProductUseCase.execute(
                    new GetProductCommand(item.productId(), CatalogView.PUBLIC));
            items.add(new CheckoutItem(item.productId(), item.quantity(), product.price()));
        }
        CheckoutCommand checkoutCommand = new CheckoutCommand(
                arguments.requireUuid("addressId"),
                paymentMethod(arguments.requireText("paymentMethod")),
                items,
                UUID.randomUUID().toString());
        String token = UUID.randomUUID().toString();
        pendingSensitiveActionStore.deleteByUserId(userId);
        pendingSensitiveActionStore.save(PendingSensitiveAction.checkout(
                token, userId, ConfirmationFingerprints.checkout(checkoutCommand), checkoutCommand));
        return ToolResult.confirmationRequired(
                "Checkout prepared for cart "
                        + cart.id()
                        + " with "
                        + items.size()
                        + " products totaling current catalog prices. Explicit confirmation is required.",
                token);
    }

    private static PaymentMethod paymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new com.superfercho.assistant.application.exception.InvalidToolArgumentsException(
                    "paymentMethod must be SIMULATED_CARD or CASH_ON_DELIVERY");
        }
    }
}
