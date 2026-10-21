"use client";

import Link from "next/link";
import type { CartItem } from "@/features/cart/api";
import { lineDisplayAmount } from "@/features/cart/api";
import { QuantityStepper } from "@/features/cart/components/quantity-stepper";
import { useRemoveCartItemMutation } from "@/features/cart/hooks";
import { cartStockWarning } from "@/features/cart/stock";
import type { Product } from "@/features/catalog/api";
import { ProductImage } from "@/features/catalog/components/product-image";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { cx } from "@/shared/utils/cx";

export function CartLine({
  item,
  product,
  busy,
}: {
  item: CartItem;
  product: Product | undefined;
  busy?: boolean;
}) {
  const removeMutation = useRemoveCartItemMutation();
  const name = product?.name ?? "Producto no disponible";
  const lineAmount = lineDisplayAmount(item);
  const currentPrice = product?.price;
  const priceChanged =
    currentPrice !== undefined &&
    (currentPrice.amount !== item.priceAtAddition.amount ||
      currentPrice.currency !== item.priceAtAddition.currency);

  const errorMessage =
    removeMutation.isError && isApiError(removeMutation.error)
      ? messageForApiProblem(removeMutation.error.problem)
      : removeMutation.isError
        ? "No se pudo quitar el producto."
        : null;
  const stockWarning = cartStockWarning(item.quantity, product?.stock);
  const outOfStock = product?.stock !== undefined && product.stock <= 0;
  const showStockDetail = Boolean(stockWarning) && !outOfStock;

  return (
    <li
      className={cx(
        "grid grid-cols-[4.5rem_minmax(0,1fr)] items-start gap-x-3 gap-y-2 rounded-2xl border border-sf-border bg-sf-surface p-3 shadow-[0_1px_2px_rgba(23,33,27,0.06)] transition-colors duration-200 hover:border-sf-primary/25",
        "md:grid-cols-[5rem_minmax(0,1fr)_auto] md:gap-4",
      )}
    >
      <div className="w-[4.5rem] shrink-0 md:w-full">
        {product ? (
          <Link href={`/products/${product.id}`} aria-label={name}>
            <ProductImage src={product.imageUrl} alt="" />
          </Link>
        ) : (
          <ProductImage src={null} alt="" />
        )}
      </div>

      <div className="min-w-0">
        {product ? (
          <Link
            href={`/products/${product.id}`}
            className="line-clamp-2 text-sm font-semibold leading-snug text-sf-ink transition-colors hover:text-sf-primary md:text-[0.95rem]"
          >
            {name}
          </Link>
        ) : (
          <p className="line-clamp-2 text-sm font-semibold leading-snug text-sf-ink md:text-[0.95rem]">
            {name}
          </p>
        )}
        {product?.brand ? (
          <p className="mt-0.5 truncate text-[11px] font-medium uppercase tracking-[0.06em] text-sf-muted">
            {product.brand}
          </p>
        ) : null}
        {priceChanged && currentPrice ? (
          <p className="mt-1 inline-flex items-center rounded-md bg-sf-yellow-soft px-2 py-0.5 text-xs font-medium text-sf-warning">
            Al agregar {formatMoney(item.priceAtAddition)} · ahora{" "}
            {formatMoney(currentPrice)}
          </p>
        ) : null}
        {!product ? (
          <p className="mt-0.5 text-xs text-sf-muted">
            {formatMoney(item.priceAtAddition)} c/u
          </p>
        ) : null}
        {outOfStock ? (
          <Badge tone="danger" className="mt-1.5 w-fit rounded-md px-2 py-0.5 text-xs">
            Agotado
          </Badge>
        ) : null}
        {showStockDetail ? (
          <p className="mt-1 text-xs font-medium text-sf-error" role="status">
            {stockWarning}
          </p>
        ) : null}
        {errorMessage ? (
          <p className="mt-1 text-xs text-sf-error">{errorMessage}</p>
        ) : null}
      </div>

      <div className="col-span-2 flex flex-wrap items-center justify-between gap-x-3 gap-y-2 md:col-span-1 md:flex-col md:items-end">
        <p className="text-lg font-bold tabular-nums text-sf-ink">
          {formatMoney(lineAmount)}
        </p>
        <div className="flex items-center gap-2">
          <QuantityStepper
            productId={item.productId}
            quantity={item.quantity}
            maxStock={product?.stock}
            disabled={busy}
          />
          <Button
            type="button"
            variant="ghost"
            className="min-h-10 px-2.5 text-sm font-semibold text-sf-error hover:bg-sf-error/5 hover:text-sf-error"
            disabled={busy || removeMutation.isPending}
            onClick={() => removeMutation.mutate(item.productId)}
          >
            {removeMutation.isPending ? "Quitando…" : "Eliminar"}
          </Button>
        </div>
      </div>
    </li>
  );
}
