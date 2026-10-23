package com.superfercho.assistant.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.assistant.application.confirmation.SensitiveActionType;
import com.superfercho.orders.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

class FerchoSensitiveActionCopyTest {

    @Test
    void pendingCopyDoesNotClaimCompletion() {
        String checkout = FerchoSensitiveActionCopy.pending(SensitiveActionType.CHECKOUT);
        String cancel = FerchoSensitiveActionCopy.pending(SensitiveActionType.CANCEL_ORDER);

        assertTrue(checkout.contains("no se ha creado"));
        assertTrue(checkout.contains("ni se ha cobrado"));
        assertTrue(cancel.contains("no se ha cancelado"));
        assertFalse(checkout.contains("Pedido confirmado"));
        assertFalse(cancel.contains("Cancelación confirmada"));
    }

    @Test
    void successCopyIncludesBackendOrderEvidence() {
        String checkout = FerchoSensitiveActionCopy.checkoutSucceeded("SF-1", OrderStatus.CONFIRMED);
        String cancel = FerchoSensitiveActionCopy.cancellationSucceeded("SF-9", OrderStatus.CANCELLED);

        assertTrue(checkout.contains("SF-1"));
        assertTrue(checkout.contains(OrderStatus.CONFIRMED.name()));
        assertTrue(cancel.contains("SF-9"));
        assertTrue(cancel.contains(OrderStatus.CANCELLED.name()));
    }
}
