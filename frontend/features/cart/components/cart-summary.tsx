"use client";

import { useState } from "react";
import Link from "next/link";
import type { Cart } from "@/features/cart/api";
import { cartLinesDisplayAmount, cartUnitCount } from "@/features/cart/api";
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
  const linesTotal = cartLinesDisplayAmount(cart);

  const errorMessage =
    clearMutation.isError && isApiError(clearMutation.error)
      ? messageForApiProblem(clearMutation.error.problem)
      : clearMutation.isError
        ? "No se pudo vaciar el carrito."
        : null;

  return (
    <Card className="grid gap-3 md:gap-4">
      <h2 className="text-lg font-semibold text-sf-ink md:text-xl">Resumen</h2>
      <dl className="grid gap-2 text-sm">
        <div className="flex justify-between gap-4">
          <dt className="text-sf-muted">Productos</dt>
          <dd className="font-semibold text-sf-ink">{cart.items.length}</dd>
        </div>
        <div className="flex justify-between gap-4">
          <dt className="text-sf-muted">Unidades</dt>
          <dd className="font-semibold text-sf-ink">{unitCount}</dd>
        </div>
        {linesTotal ? (
          <div className="flex justify-between gap-4 border-t border-sf-border pt-3">
            <dt className="text-base font-semibold text-sf-ink">Subtotal</dt>
            <dd className="text-lg font-bold text-sf-ink">
              {formatMoney(linesTotal)}
            </dd>
          </div>
        ) : null}
      </dl>
      <p className="text-xs leading-relaxed text-sf-muted">
        Precios al agregar. Sin envío, impuestos ni descuentos.
      </p>
      {errorMessage ? (
        <Alert tone="error" title="No se pudo vaciar">
          {errorMessage}
        </Alert>
      ) : null}
      <Link href="/checkout" className={buttonClassName("primary")}>
        Ir a pagar
      </Link>
      <Link
        href="/catalog"
        className="inline-flex min-h-11 items-center justify-center text-sm font-semibold text-sf-muted hover:text-sf-primary hover:underline"
      >
        Seguir comprando
      </Link>
      {confirming ? (
        <div className="grid gap-2 border-t border-sf-border pt-3">
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
