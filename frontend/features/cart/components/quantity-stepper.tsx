"use client";

import { useState } from "react";
import { canIncreaseCartQuantity } from "@/features/cart/stock";
import { useChangeCartItemQuantityMutation } from "@/features/cart/hooks";
import { Button } from "@/shared/ui/button";
import { cx } from "@/shared/utils/cx";

export function QuantityStepper({
  productId,
  quantity,
  maxStock,
  disabled,
}: {
  productId: string;
  quantity: number;
  maxStock?: number;
  disabled?: boolean;
}) {
  const changeMutation = useChangeCartItemQuantityMutation();
  const [draft, setDraft] = useState(String(quantity));
  const [syncedQuantity, setSyncedQuantity] = useState(quantity);
  const busy = disabled || changeMutation.isPending;

  if (syncedQuantity !== quantity) {
    setSyncedQuantity(quantity);
    setDraft(String(quantity));
  }

  async function commit(next: number) {
    if (busy || next < 1 || next === quantity) {
      setDraft(String(quantity));
      return;
    }
    if (maxStock !== undefined && next > maxStock) {
      setDraft(String(quantity));
      return;
    }
    try {
      await changeMutation.mutateAsync({ productId, quantity: next });
    } catch {
      setDraft(String(quantity));
    }
  }

  return (
    <div className="flex items-center gap-2">
      <Button
        type="button"
        variant="secondary"
        className="min-h-11 w-11 px-0"
        aria-label="Disminuir cantidad"
        disabled={busy || quantity <= 1}
        onClick={() => void commit(quantity - 1)}
      >
        −
      </Button>
      <input
        id={`cart-qty-${productId}`}
        aria-label="Cantidad"
        inputMode="numeric"
        pattern="[0-9]*"
        className={cx(
          "h-11 w-14 rounded-lg border border-sf-border bg-sf-surface text-center text-base font-semibold text-sf-ink",
          "disabled:cursor-not-allowed disabled:text-sf-muted",
        )}
        value={draft}
        disabled={busy}
        onChange={(event) => setDraft(event.target.value)}
        onBlur={() => {
          const parsed = Number.parseInt(draft, 10);
          void commit(Number.isFinite(parsed) ? parsed : quantity);
        }}
        onKeyDown={(event) => {
          if (event.key === "Enter") {
            event.currentTarget.blur();
          }
        }}
      />
      <Button
        type="button"
        variant="secondary"
        className="min-h-11 w-11 px-0"
        aria-label="Aumentar cantidad"
        disabled={busy || !canIncreaseCartQuantity(quantity, maxStock)}
        onClick={() => void commit(quantity + 1)}
      >
        +
      </Button>
    </div>
  );
}
