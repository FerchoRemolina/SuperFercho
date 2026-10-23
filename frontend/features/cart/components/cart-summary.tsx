"use client";

import { useState } from "react";
import Link from "next/link";
import type { Cart } from "@/features/cart/api";
import { cartUnitCount, officialCartQuote } from "@/features/cart/api";
import { useClearCartMutation } from "@/features/cart/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";

export function CartSummary({ cart }: { cart: Cart }) {
  const clearMutation = useClearCartMutation();
  const [confirming, setConfirming] = useState(false);
  const unitCount = cartUnitCount(cart);
  const quote = officialCartQuote(cart);

  const errorMessage =
    clearMutation.isError && isApiError(clearMutation.error)
      ? messageForApiProblem(clearMutation.error.problem)
      : clearMutation.isError
        ? "No se pudo vaciar el carrito."
        : null;

  return (
    <Card className="grid gap-2.5 !p-4 md:gap-3 md:!p-5">
      <h2 className="text-base font-semibold text-sf-ink md:text-lg">Resumen</h2>
      <dl className="grid gap-1.5 text-sm">
        <div className="flex justify-between gap-4">
          <dt className="text-sf-muted">Productos</dt>
          <dd className="font-semibold tabular-nums text-sf-ink">
            {cart.items.length}
          </dd>
        </div>
        <div className="flex justify-between gap-4">
          <dt className="text-sf-muted">Unidades</dt>
          <dd className="font-semibold tabular-nums text-sf-ink">{unitCount}</dd>
        </div>
        {quote ? (
          <div className="mt-1 flex items-baseline justify-between gap-4 border-t border-sf-border pt-2.5">
            <dt className="text-base font-semibold text-sf-ink">Estimado vigente</dt>
            <dd className="text-xl font-bold tabular-nums text-sf-ink">
              {formatMoney(quote)}
            </dd>
          </div>
        ) : cart.items.length > 0 ? (
          <p className="mt-1 border-t border-sf-border pt-2.5 text-sm text-sf-muted">
            No hay un total oficial: falta el precio vigente de al menos un producto.
          </p>
        ) : null}
      </dl>
      <p className="text-xs leading-relaxed text-sf-muted">
        Precio actual del catálogo. El cobro lo confirma el checkout. Sin envío, impuestos ni descuentos.
      </p>
      {errorMessage ? (
        <Alert tone="error" title="No se pudo vaciar">
          {errorMessage}
        </Alert>
      ) : null}
      <Link href="/checkout" className={buttonClassName("primary", "w-full")}>
        Ir a pagar
      </Link>
      <Link
        href="/catalog"
        className="inline-flex min-h-10 items-center justify-center text-sm font-semibold text-sf-muted transition-colors hover:text-sf-primary hover:underline"
      >
        Seguir comprando
      </Link>
      {confirming ? (
        <div className="grid gap-2 rounded-xl bg-sf-bg p-3">
          <p className="text-sm font-semibold text-sf-ink">
            ¿Vaciar el carrito?
          </p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button
              type="button"
              variant="destructive"
              className="flex-1"
              disabled={clearMutation.isPending}
              onClick={() => clearMutation.mutate()}
            >
              {clearMutation.isPending ? "Vaciando…" : "Sí, vaciar"}
            </Button>
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              disabled={clearMutation.isPending}
              onClick={() => setConfirming(false)}
            >
              Conservar productos
            </Button>
          </div>
        </div>
      ) : (
        <Button
          type="button"
          variant="ghost"
          className="justify-center px-0 text-sm font-semibold text-sf-muted hover:bg-transparent hover:text-sf-error"
          onClick={() => setConfirming(true)}
        >
          Vaciar carrito
        </Button>
      )}
    </Card>
  );
}
