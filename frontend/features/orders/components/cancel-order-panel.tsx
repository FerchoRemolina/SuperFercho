"use client";

import { useEffect, useState } from "react";
import type { Order } from "@/features/orders/api";
import { useCancelOrderMutation } from "@/features/orders/hooks";
import {
  CANCEL_CONFIRMATION_BODY,
  CANCEL_CONFIRMATION_TITLE,
  CANCEL_WINDOW_EXPIRED_COPY,
  CANCEL_WINDOW_IDLE_COPY,
  cancelErrorCopy,
  cancellationRemainingLabel,
  cancelPanelState,
  isCancelSubmitLocked,
} from "@/features/orders/order-views";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { cx } from "@/shared/utils/cx";

export function CancelOrderPanel({ order }: { order: Order }) {
  const cancelMutation = useCancelOrderMutation();
  const [confirming, setConfirming] = useState(false);
  const [now, setNow] = useState(() => new Date());
  const locked = isCancelSubmitLocked(cancelMutation.isPending);
  const state = cancelPanelState({
    order,
    confirming,
    isPending: cancelMutation.isPending,
    now,
  });

  useEffect(() => {
    if (order.status !== "PENDING") {
      return;
    }
    const id = window.setInterval(() => setNow(new Date()), 30_000);
    return () => window.clearInterval(id);
  }, [order.status, order.createdAt]);

  if (state === "hidden") {
    return null;
  }

  const error = cancelMutation.isError
    ? cancelErrorCopy(cancelMutation.error)
    : null;
  const remaining = cancellationRemainingLabel(order.createdAt, now);
  const actionable =
    state === "idle" || state === "confirming" || state === "pending";

  return (
    <Card
      className={cx(
        "grid gap-2.5 !p-4 md:!p-4",
        !actionable && "border-sf-border bg-sf-bg/60 shadow-none",
      )}
    >
      {error ? (
        <Alert tone="error" title={error.title}>
          {error.message}
        </Alert>
      ) : null}

      {state === "expired" ? (
        <p className="text-xs leading-relaxed text-sf-muted md:text-sm">
          {CANCEL_WINDOW_EXPIRED_COPY}
        </p>
      ) : null}

      {state === "idle" ? (
        <>
          <p className="text-xs leading-relaxed text-sf-muted md:text-sm">
            {CANCEL_WINDOW_IDLE_COPY}
          </p>
          {remaining ? (
            <p className="text-sm font-semibold text-sf-ink">{remaining}</p>
          ) : null}
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
      ) : null}

      {state === "confirming" || state === "pending" ? (
        <>
          <p className="text-sm font-semibold text-sf-ink">
            {CANCEL_CONFIRMATION_TITLE}
          </p>
          <p className="text-xs leading-relaxed text-sf-muted md:text-sm">
            {CANCEL_CONFIRMATION_BODY}
          </p>
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
      ) : null}
    </Card>
  );
}
