"use client";

import { useState } from "react";
import type { Order } from "@/features/orders/api";
import { useCancelOrderMutation } from "@/features/orders/hooks";
import {
  CANCEL_CONFIRMATION_BODY,
  CANCEL_CONFIRMATION_TITLE,
  cancelErrorCopy,
  cancelPanelState,
  isCancelSubmitLocked,
} from "@/features/orders/order-views";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";

export function CancelOrderPanel({ order }: { order: Order }) {
  const cancelMutation = useCancelOrderMutation();
  const [confirming, setConfirming] = useState(false);
  const locked = isCancelSubmitLocked(cancelMutation.isPending);
  const state = cancelPanelState({
    order,
    confirming,
    isPending: cancelMutation.isPending,
  });

  if (state === "hidden") {
    return null;
  }

  const error = cancelMutation.isError
    ? cancelErrorCopy(cancelMutation.error)
    : null;

  return (
    <div className="grid gap-3 rounded-2xl border border-dashed border-sf-border bg-sf-surface p-4">
      {error ? (
        <Alert tone="error" title={error.title}>
          {error.message}
        </Alert>
      ) : null}

      {state === "idle" ? (
        <>
          <p className="text-sm text-sf-muted">
            Puedes cancelar este pedido mientras el supermercado aún no lo
            confirme.
          </p>
          <Button
            type="button"
            variant="ghost"
            className="justify-start px-0 text-sf-error hover:bg-red-50"
            disabled={locked}
            onClick={() => {
              cancelMutation.reset();
              setConfirming(true);
            }}
          >
            Cancelar pedido
          </Button>
        </>
      ) : (
        <>
          <p className="text-sm font-semibold text-sf-ink">
            {CANCEL_CONFIRMATION_TITLE}
          </p>
          <p className="text-sm text-sf-muted">{CANCEL_CONFIRMATION_BODY}</p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button
              type="button"
              variant="destructive"
              disabled={locked}
              onClick={() => {
                if (locked) {
                  return;
                }
                cancelMutation.mutate(order.id, {
                  onSuccess: () => setConfirming(false),
                });
              }}
            >
              {locked ? "Cancelando…" : "Sí, cancelar"}
            </Button>
            <Button
              type="button"
              variant="secondary"
              disabled={locked}
              onClick={() => setConfirming(false)}
            >
              Conservar pedido
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
