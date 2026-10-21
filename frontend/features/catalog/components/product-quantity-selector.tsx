"use client";

import {
  canDecrementSelectedQuantity,
  canIncrementSelectedQuantity,
  nextSelectedQuantity,
} from "@/features/catalog/quantity";
import { Button } from "@/shared/ui/button";

export function ProductQuantitySelector({
  quantity,
  stock,
  onChange,
  disabled,
}: {
  quantity: number;
  stock: number;
  onChange: (next: number) => void;
  disabled?: boolean;
}) {
  const busy = Boolean(disabled);

  return (
    <div className="flex items-center justify-center gap-1.5">
      <div className="flex min-h-11 items-center gap-1 rounded-full border border-sf-border bg-sf-bg p-1">
        <Button
          type="button"
          variant="ghost"
          className="size-9 rounded-full p-0 hover:bg-sf-surface disabled:opacity-40"
          aria-label="Disminuir cantidad"
          disabled={busy || !canDecrementSelectedQuantity(quantity)}
          onClick={() => onChange(nextSelectedQuantity(quantity, -1, stock))}
        >
          −
        </Button>
        <span
          className="min-w-7 text-center text-sm font-bold tabular-nums text-sf-ink"
          aria-live="polite"
          aria-label="Cantidad"
        >
          {quantity}
        </span>
        <Button
          type="button"
          variant="ghost"
          className="size-9 rounded-full p-0 hover:bg-sf-surface disabled:opacity-40"
          aria-label="Aumentar cantidad"
          disabled={busy || !canIncrementSelectedQuantity(quantity, stock)}
          onClick={() => onChange(nextSelectedQuantity(quantity, 1, stock))}
        >
          +
        </Button>
      </div>
    </div>
  );
}
