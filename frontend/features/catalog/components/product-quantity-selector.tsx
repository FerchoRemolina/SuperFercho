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
    <div className="flex items-center gap-2">
      <Button
        type="button"
        variant="secondary"
        className="min-h-11 w-11 px-0"
        aria-label="Disminuir cantidad"
        disabled={busy || !canDecrementSelectedQuantity(quantity)}
        onClick={() => onChange(nextSelectedQuantity(quantity, -1, stock))}
      >
        −
      </Button>
      <span
        className="inline-flex h-11 min-w-14 items-center justify-center rounded-lg border border-sf-border bg-sf-surface px-2 text-center text-base font-semibold text-sf-ink"
        aria-live="polite"
        aria-label="Cantidad"
      >
        {quantity}
      </span>
      <Button
        type="button"
        variant="secondary"
        className="min-h-11 w-11 px-0"
        aria-label="Aumentar cantidad"
        disabled={busy || !canIncrementSelectedQuantity(quantity, stock)}
        onClick={() => onChange(nextSelectedQuantity(quantity, 1, stock))}
      >
        +
      </Button>
    </div>
  );
}
