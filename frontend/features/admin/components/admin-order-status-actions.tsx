"use client";

import { useState } from "react";
import { useUpdateAdminOrderStatusMutation } from "@/features/admin/hooks";
import {
  adminOrderStatusAdvanceConfirmation,
  adminOrderStatusAdvanceLabel,
  adminOrderStatusPanelState,
  isAdminOrderStatusSubmitLocked,
  nextAdminOrderStatus,
} from "@/features/admin/presentation";
import {
  orderStatusLabel,
  type Order,
  type OrderStatus,
} from "@/features/orders/api";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";

export function AdminOrderStatusActions({ order }: { order: Order }) {
  const mutation = useUpdateAdminOrderStatusMutation();
  const [confirming, setConfirming] = useState(false);
  const nextStatus = nextAdminOrderStatus(order.status);
  const actionLabel = adminOrderStatusAdvanceLabel(order.status);
  const locked = isAdminOrderStatusSubmitLocked(mutation.isPending);
  const panelState = adminOrderStatusPanelState({
    status: order.status,
    confirming,
    isPending: mutation.isPending,
  });

  if (panelState === "hidden" || nextStatus === null || actionLabel === null) {
    return (
      <p className="text-sm text-sf-muted">
        No hay cambios de estado disponibles para este pedido.
      </p>
    );
  }

  const confirmation = adminOrderStatusAdvanceConfirmation({
    orderNumber: order.orderNumber,
    currentStatus: order.status,
    nextStatus,
  });

  const errorMessage = mutation.isError
    ? isApiError(mutation.error)
      ? messageForApiProblem(mutation.error.problem)
      : "No se pudo actualizar el estado del pedido."
    : null;

  return (
    <div className="grid gap-3">
      <p className="text-sm text-sf-muted">
        Estado actual: {orderStatusLabel(order.status)}. Siguiente paso:{" "}
        {orderStatusLabel(nextStatus)}.
      </p>

      {errorMessage ? (
        <Alert tone="error" title="No se pudo cambiar el estado">
          {errorMessage}
        </Alert>
      ) : null}

      {panelState === "idle" ? (
        <Button
          type="button"
          variant="primary"
          disabled={locked}
          onClick={() => {
            mutation.reset();
            setConfirming(true);
          }}
        >
          {actionLabel}
        </Button>
      ) : (
        <div className="grid gap-2 rounded-xl border border-sf-border bg-sf-bg p-3">
          <p className="text-sm font-semibold text-sf-ink">{confirmation.title}</p>
          <p className="text-sm text-sf-muted">{confirmation.body}</p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button
              type="button"
              variant="primary"
              disabled={locked}
              onClick={() => {
                if (locked) {
                  return;
                }
                const status: OrderStatus = nextStatus;
                mutation.mutate(
                  { orderId: order.id, body: { status } },
                  {
                    onSuccess: () => setConfirming(false),
                  },
                );
              }}
            >
              {locked ? "Actualizando…" : "Sí, cambiar estado"}
            </Button>
            <Button
              type="button"
              variant="secondary"
              disabled={locked}
              onClick={() => setConfirming(false)}
            >
              Cancelar
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
