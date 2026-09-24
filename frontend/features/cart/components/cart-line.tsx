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
        "grid gap-3 rounded-2xl border border-sf-border bg-sf-surface p-3",
        "md:grid-cols-[5rem_minmax(0,1fr)_auto] md:items-start md:gap-4 md:p-4",
      )}
    >
      <div className="flex gap-3 md:contents">
        <div className="w-[4.5rem] shrink-0 md:w-full">
          {product ? (
            <Link href={`/products/${product.id}`} aria-label={name}>
              <ProductImage src={product.imageUrl} alt="" />
            </Link>
          ) : (
            <ProductImage src={null} alt="" />
          )}
        </div>

        <div className="min-w-0 flex-1">
          {product ? (
            <Link
              href={`/products/${product.id}`}
              className="line-clamp-2 text-base font-semibold text-sf-ink hover:text-sf-primary"
            >
              {name}
            </Link>
          ) : (
            <p className="line-clamp-2 text-base font-semibold text-sf-ink">
              {name}
            </p>
          )}
          {product?.brand ? (
            <p className="mt-0.5 truncate text-sm text-sf-muted">{product.brand}</p>
          ) : null}
          {priceChanged && currentPrice ? (
            <p className="mt-1 text-xs text-sf-muted">
              Al agregar {formatMoney(item.priceAtAddition)} · ahora{" "}
              {formatMoney(currentPrice)}
            </p>
          ) : null}
          {!product ? (
            <p className="mt-1 text-xs text-sf-muted">
              {formatMoney(item.priceAtAddition)} c/u
            </p>
          ) : null}
          {outOfStock ? (
            <Badge tone="danger" className="mt-2 w-fit">
              Agotado
            </Badge>
          ) : null}
          {showStockDetail ? (
            <p className="mt-1.5 text-xs text-sf-error" role="status">
              {stockWarning}
            </p>
          ) : null}
          {errorMessage ? (
            <p className="mt-1.5 text-xs text-sf-error">{errorMessage}</p>
          ) : null}
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 md:flex-col md:items-end md:gap-2">
        <p className="text-base font-bold text-sf-ink md:text-lg">
          {formatMoney(lineAmount)}
        </p>
        <div className="flex items-center gap-3 md:flex-col md:items-end md:gap-2">
          <QuantityStepper
            productId={item.productId}
            quantity={item.quantity}
            maxStock={product?.stock}
            disabled={busy}
          />
          <Button
            type="button"
            variant="ghost"
            className="min-h-11 px-0 text-sm font-semibold text-sf-error hover:bg-sf-error/5 hover:text-sf-error"
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
