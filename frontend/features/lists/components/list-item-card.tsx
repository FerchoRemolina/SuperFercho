"use client";

import { useState } from "react";
import Link from "next/link";
import { useAddCartItemMutation } from "@/features/cart/hooks";
import {
  isProductAvailable,
  type Product,
} from "@/features/catalog/api";
import { ProductImage } from "@/features/catalog/components/product-image";
import {
  canOfferAddToCart,
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
  const addCartMutation = useAddCartItemMutation();
  const [addedFeedback, setAddedFeedback] = useAddedFeedback();

  const purchasable = product !== undefined && product.status === "ACTIVE";
  const stock = product?.stock;
  const offerAddToCart = canOfferAddToCart(purchasable, stock);
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

  const cartError =
    addCartMutation.isError && isApiError(addCartMutation.error)
      ? messageForApiProblem(addCartMutation.error.problem)
      : addCartMutation.isError
        ? "No se pudo añadir al carrito."
        : null;

  async function onAddToCart() {
    try {
      await addCartMutation.mutateAsync({
        productId: item.productId,
        quantity: item.quantity,
      });
      setAddedFeedback();
    } catch {
      // surfaced below
    }
  }

  const name = product?.name ?? "Producto no disponible";

  return (
    <li
      className={cx(
        "grid gap-3 rounded-2xl border border-sf-border bg-sf-surface p-3",
        "md:grid-cols-[5rem_minmax(0,1fr)] md:items-start md:gap-4 md:p-4",
      )}
    >
      <div className="flex gap-3 md:contents">
        <div className="w-[3.75rem] shrink-0 sm:w-[4.5rem] md:w-full">
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
            <>
              {purchasable ? (
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
              {product.brand ? (
                <p className="mt-0.5 truncate text-sm text-sf-muted">
                  {product.brand}
                </p>
              ) : null}
              <p className="mt-1 text-base font-bold text-sf-ink">
                {formatMoney(product.price)}
              </p>
              <div className="mt-1.5">
                <Badge
                  tone={availability === "Disponible" ? "primary" : "danger"}
                  className="w-fit"
                >
                  {availability}
                </Badge>
              </div>
              {showStockDetail ? (
                <p className="mt-1 text-xs text-sf-muted">{stockLabel}</p>
              ) : null}
              {!purchasable ? (
                <p className="mt-1 text-xs text-sf-muted md:text-sm">
                  Este producto no está a la venta ahora. Puedes mantenerlo o
                  quitarlo de la lista.
                </p>
              ) : null}
              {purchasable && outOfStock ? (
                <p className="mt-1 text-xs text-sf-muted md:text-sm">
                  Este producto está agotado por ahora. Puedes mantenerlo en la
                  lista.
                </p>
              ) : null}
            </>
          ) : (
            <>
              <p className="line-clamp-2 text-base font-semibold text-sf-ink">
                {name}
              </p>
              <p className="mt-1 text-xs text-sf-muted md:text-sm">
                Ya no está en el catálogo o no se puede comprar ahora. Puedes
                quitarlo de la lista.
              </p>
              <Badge tone="danger" className="mt-1.5 w-fit">
                No disponible
              </Badge>
            </>
          )}

          {removeError ? (
            <p className="mt-1.5 text-xs text-sf-error">{removeError}</p>
          ) : null}
          {cartError ? (
            <p className="mt-1.5 text-xs text-sf-error">{cartError}</p>
          ) : null}
          {addedFeedback ? (
            <p
              className="mt-1.5 text-sm font-semibold text-sf-success"
              aria-live="polite"
            >
              Añadido al carrito.{" "}
              <Link
                href="/cart"
                className="font-semibold text-sf-primary hover:underline"
              >
                Ver carrito
              </Link>
            </p>
          ) : null}

          <div className="mt-2.5 flex items-center gap-2 overflow-x-auto sm:gap-3">
            <ListQuantityStepper
              shoppingListId={shoppingListId}
              productId={item.productId}
              quantity={item.quantity}
              disabled={busy || removeMutation.isPending}
            />
            {offerAddToCart ? (
              <Button
                type="button"
                variant="secondary"
                className="min-h-11 shrink-0 px-3 text-sm sm:px-4"
                disabled={
                  busy || addCartMutation.isPending || removeMutation.isPending
                }
                onClick={() => void onAddToCart()}
              >
                {addCartMutation.isPending
                  ? "Añadiendo…"
                  : "Agregar al carrito"}
              </Button>
            ) : null}
            <Button
              type="button"
              variant="ghost"
              className="min-h-11 shrink-0 px-2 text-sm font-semibold text-sf-error hover:bg-sf-error/5 hover:text-sf-error sm:px-3"
              disabled={busy || removeMutation.isPending}
              onClick={() => void removeMutation.mutateAsync(item.productId)}
            >
              {removeMutation.isPending ? "Eliminando…" : "Eliminar"}
            </Button>
          </div>
        </div>
      </div>
    </li>
  );
}

function useAddedFeedback() {
  const [added, setAdded] = useState(false);
  return [
    added,
    () => {
      setAdded(true);
      window.setTimeout(() => setAdded(false), 2000);
    },
  ] as const;
}
