package com.superfercho.assistant.application.confirmation;

import com.superfercho.orders.application.dto.CheckoutCommand;
import java.util.UUID;

public record PendingSensitiveAction(
        String token,
        UUID userId,
        SensitiveActionType type,
        String fingerprint,
        CheckoutCommand checkoutCommand,
        UUID orderId) {

    public static PendingSensitiveAction checkout(
            String token, UUID userId, String fingerprint, CheckoutCommand checkoutCommand) {
        return new PendingSensitiveAction(
                token, userId, SensitiveActionType.CHECKOUT, fingerprint, checkoutCommand, null);
    }

    public static PendingSensitiveAction cancelOrder(String token, UUID userId, String fingerprint, UUID orderId) {
        return new PendingSensitiveAction(
                token, userId, SensitiveActionType.CANCEL_ORDER, fingerprint, null, orderId);
    }
}
