package com.superfercho.assistant.application.service;

import com.superfercho.assistant.application.confirmation.SensitiveActionType;
import com.superfercho.orders.domain.model.OrderStatus;

/**
 * Textos de usuario para operaciones sensibles. Solo el backend los emite:
 * preparar no equivale a ejecutar, y el modelo no puede afirmar el resultado.
 */
public final class FerchoSensitiveActionCopy {

    private FerchoSensitiveActionCopy() {}

    public static String pending(SensitiveActionType type) {
        if (type == SensitiveActionType.CANCEL_ORDER) {
            return "La cancelación quedó preparada, pero el pedido no se ha cancelado. "
                    + "Confírmala con el botón de confirmación; un «sí» por chat no ejecuta la operación.";
        }
        return "El pedido quedó preparado, pero no se ha creado ni se ha cobrado. "
                + "Confírmalo con el botón de confirmación; un «sí» por chat no ejecuta la operación.";
    }

    public static String checkoutSucceeded(String orderNumber, OrderStatus status) {
        return "Pedido confirmado: número " + orderNumber + ", estado " + status + ".";
    }

    public static String cancellationSucceeded(String orderNumber, OrderStatus status) {
        return "Cancelación confirmada: número " + orderNumber + ", estado " + status + ".";
    }
}
