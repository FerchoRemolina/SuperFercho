"use client";

import Link from "next/link";
import {
  isProductAvailable,
  type Product,
} from "@/features/catalog/api";
import { ProductImage } from "@/features/catalog/components/product-image";
import {
  productAvailabilityLabel,
  productStockLabel,
} from "@/features/catalog/quantity";
import type { ShoppingListItem } from "@/features/lists/api";
import { ListQuantityStepper } from "@/features/lists/components/list-quantity-stepper";
import { useRemoveShoppingListItemMutation } from "@/features/lists/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { cx } from "@/shared/utils/cx";

export function ListItemCard({
  shoppingListId,
  item,
  product,
  busy,
}: {
  shoppingListId: string;
  item: ShoppingListItem;
  product: Product | undefined;
  busy?: boolean;
}) {
  const removeMutation = useRemoveShoppingListItemMutation(shoppingListId);
  const notBuying = item.quantity === 0;

  const purchasable = product !== undefined && product.status === "ACTIVE";
  const stock = product?.stock;
  const outOfStock = stock !== undefined && stock <= 0;
  const availability = product
    ? productAvailabilityLabel(product.status, product.stock)
    : null;
  const stockLabel = product ? productStockLabel(product.stock) : null;
  const showStockDetail =
    purchasable &&
    !outOfStock &&
    product !== undefined &&
    isProductAvailable(product) &&
    (stock ?? 0) > 0;

  const removeError =
    removeMutation.isError && isApiError(removeMutation.error)
      ? messageForApiProblem(removeMutation.error.problem)
      : removeMutation.isError
        ? "No se pudo eliminar el producto."
        : null;

  const name = product?.name ?? "Producto no disponible";
  const nameBody = purchasable && product ? (
    <Link
      href={`/products/${product.id}`}
      className="line-clamp-1 text-sm font-semibold leading-snug text-sf-ink transition-colors hover:text-sf-primary md:text-[0.95rem]"
    >
      {name}
    </Link>
  ) : (
    <p className="line-clamp-1 text-sm font-semibold leading-snug text-sf-ink md:text-[0.95rem]">
      {name}
    </p>
  );

  return (
    <li
      className={cx(
        "grid grid-cols-[3.5rem_minmax(0,1fr)] items-start gap-x-3 gap-y-2 rounded-xl border p-3",
        "shadow-[0_1px_2px_rgba(23,33,27,0.05)] transition-colors duration-200",
        notBuying
          ? "border-sf-border/70 bg-sf-bg/60"
          : "border-sf-border bg-sf-surface hover:border-sf-primary/25",
      )}
    >
      <div className="w-[3.5rem]">
        {product ? (
          <Link href={`/products/${product.id}`} aria-label={name}>
            <ProductImage src={product.imageUrl} alt="" />
          </Link>
        ) : (
          <ProductImage src={null} alt="" />
        )}
      </div>

      <div className="min-w-0">
        {nameBody}
        {product ? (
          <>
            {product.brand ? (
              <p className="mt-0.5 truncate text-[0.7rem] font-medium uppercase tracking-[0.06em] text-sf-muted">
                {product.brand}
              </p>
            ) : null}
            <div className="mt-0.5 flex flex-wrap items-center gap-x-2.5 gap-y-1">
              <p className="text-base font-bold tabular-nums text-sf-ink">
                {formatMoney(product.price)}
              </p>
              <Badge
                tone={availability === "Disponible" ? "primary" : "danger"}
                className="rounded-md px-1.5 py-0.5 text-[0.7rem]"
              >
                {availability}
              </Badge>
              {notBuying ? (
                <Badge
                  tone="neutral"
                  className="rounded-md px-1.5 py-0.5 text-[0.7rem]"
                >
                  Guardado · no se compra ahora
                </Badge>
              ) : null}
            </div>
            {showStockDetail ? (
              <p className="mt-0.5 text-xs text-sf-muted">{stockLabel}</p>
            ) : null}
            {!purchasable ? (
              <p className="mt-0.5 text-xs text-sf-muted">
                No está a la venta ahora; puedes mantenerlo o quitarlo.
              </p>
            ) : null}
            {purchasable && outOfStock ? (
              <p className="mt-0.5 text-xs text-sf-muted">
                Está agotado por ahora; puedes mantenerlo en la lista.
              </p>
            ) : null}
          </>
        ) : (
          <>
            <p className="mt-0.5 text-xs text-sf-muted">
              Ya no está en el catálogo. Puedes quitarlo de la lista.
            </p>
            <Badge
              tone="danger"
              className="mt-1 rounded-md px-1.5 py-0.5 text-[0.7rem]"
            >
              No disponible
            </Badge>
          </>
        )}
        {removeError ? (
          <p className="mt-1 text-xs text-sf-error">{removeError}</p>
        ) : null}
      </div>

      <div className="col-span-2 flex items-center justify-between gap-2 border-t border-sf-border/70 pt-2">
        <ListQuantityStepper
          shoppingListId={shoppingListId}
          productId={item.productId}
          quantity={item.quantity}
          disabled={busy || removeMutation.isPending}
        />
        <Button
          type="button"
          variant="ghost"
          className="min-h-10 shrink-0 px-2 text-sm font-semibold text-sf-error hover:bg-sf-error/5 hover:text-sf-error sm:px-2.5"
          disabled={busy || removeMutation.isPending}
          onClick={() => void removeMutation.mutateAsync(item.productId)}
        >
          {removeMutation.isPending ? "Eliminando…" : "Eliminar"}
        </Button>
      </div>
    </li>
  );
}
