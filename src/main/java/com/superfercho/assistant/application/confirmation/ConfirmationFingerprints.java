package com.superfercho.assistant.application.confirmation;

import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutItem;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ConfirmationFingerprints {

    private ConfirmationFingerprints() {}

    public static String checkout(CheckoutCommand command) {
        String items = command.items().stream()
                .map(ConfirmationFingerprints::item)
                .collect(Collectors.joining(","));
        return command.addressId()
                + "|"
                + command.paymentMethod()
                + "|"
                + command.idempotencyKey()
                + "|"
                + items;
    }

    public static String cancelOrder(UUID orderId) {
        return "CANCEL|" + orderId;
    }

    private static String item(CheckoutItem item) {
        return item.productId() + ":" + item.quantity() + ":" + item.expectedUnitPrice().amount();
    }
}
