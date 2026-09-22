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

  return (
    <li
      className={cx(
        "grid gap-4 rounded-2xl border border-sf-border bg-sf-surface p-4",
        "md:grid-cols-[6rem_minmax(0,1fr)_auto] md:items-start",
      )}
    >
      <div className="w-24 md:w-full">
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
            className="text-base font-semibold text-sf-ink hover:text-sf-primary"
          >
            {name}
          </Link>
        ) : (
          <p className="text-base font-semibold text-sf-ink">{name}</p>
        )}
        {product?.brand ? (
          <p className="mt-1 text-sm text-sf-muted">{product.brand}</p>
        ) : null}
        <p className="mt-2 text-sm text-sf-muted">
          Precio al agregar: {formatMoney(item.priceAtAddition)}
        </p>
        {priceChanged && currentPrice ? (
          <p className="text-sm text-sf-muted">
            Precio actual: {formatMoney(currentPrice)}
          </p>
        ) : null}
        {errorMessage ? (
          <p className="mt-2 text-sm text-sf-error">{errorMessage}</p>
        ) : null}
        {stockWarning ? (
          <p className="mt-2 text-sm text-sf-error" role="status">
            {stockWarning}
          </p>
        ) : null}
      </div>

      <div className="flex flex-col gap-3 md:items-end">
        <p className="text-base font-bold text-sf-ink">
          {formatMoney(lineAmount)}
        </p>
        <QuantityStepper
          productId={item.productId}
          quantity={item.quantity}
          maxStock={product?.stock}
          disabled={busy}
        />
        <Button
          type="button"
          variant="ghost"
          className="justify-start px-0 md:justify-end"
          disabled={busy || removeMutation.isPending}
          onClick={() => removeMutation.mutate(item.productId)}
        >
          {removeMutation.isPending ? "Quitando…" : "Eliminar"}
        </Button>
      </div>
    </li>
  );
}
