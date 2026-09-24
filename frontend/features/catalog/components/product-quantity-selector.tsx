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
      <Button
        type="button"
        variant="secondary"
        className="min-h-11 w-10 px-0 text-base"
        aria-label="Disminuir cantidad"
        disabled={busy || !canDecrementSelectedQuantity(quantity)}
        onClick={() => onChange(nextSelectedQuantity(quantity, -1, stock))}
      >
        −
      </Button>
      <span
        className="inline-flex h-11 min-w-10 items-center justify-center rounded-lg border border-sf-border bg-sf-bg px-2 text-center text-sm font-semibold text-sf-ink"
        aria-live="polite"
        aria-label="Cantidad"
      >
        {quantity}
      </span>
      <Button
        type="button"
        variant="secondary"
        className="min-h-11 w-10 px-0 text-base"
        aria-label="Aumentar cantidad"
        disabled={busy || !canIncrementSelectedQuantity(quantity, stock)}
        onClick={() => onChange(nextSelectedQuantity(quantity, 1, stock))}
      >
        +
      </Button>
    </div>
  );
}
