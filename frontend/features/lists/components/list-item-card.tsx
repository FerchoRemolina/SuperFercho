"use client";

import { useState } from "react";
import Link from "next/link";
import { useAddCartItemMutation } from "@/features/cart/hooks";
import {
  isProductAvailable,
  type Product,
} from "@/features/catalog/api";
import { ProductImage } from "@/features/catalog/components/product-image";
import { productStockLabel } from "@/features/catalog/quantity";
import type { ShoppingListItem } from "@/features/lists/api";
import { ListQuantityStepper } from "@/features/lists/components/list-quantity-stepper";
import { useRemoveShoppingListItemMutation } from "@/features/lists/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";

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
  const stockLabel = product ? productStockLabel(product.stock) : null;

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

  return (
    <Card className="grid gap-4 p-4 md:grid-cols-[8rem_1fr]">
      <div>
        {product ? (
          <Link href={`/products/${product.id}`} aria-label={product.name}>
            <ProductImage src={product.imageUrl} alt="" />
          </Link>
        ) : (
          <ProductImage src={null} alt="" />
        )}
      </div>

      <div className="flex min-w-0 flex-col gap-3">
        {product ? (
          <>
            {product.brand ? (
              <p className="text-sm text-sf-muted">{product.brand}</p>
            ) : null}
            <h2 className="text-lg font-semibold text-sf-ink">
              {purchasable ? (
                <Link
                  href={`/products/${product.id}`}
                  className="hover:text-sf-primary"
                >
                  {product.name}
                </Link>
              ) : (
                product.name
              )}
            </h2>
            <p className="text-base font-bold text-sf-ink">
              {formatMoney(product.price)}
            </p>
            <div className="flex flex-wrap gap-2">
              <Badge tone={purchasable ? "primary" : "danger"} className="w-fit">
                {purchasable ? "Disponible" : "No disponible"}
              </Badge>
              {stockLabel ? (
                <Badge
                  tone={isProductAvailable(product) ? "primary" : "danger"}
                  className="w-fit"
                >
                  {stockLabel}
                </Badge>
              ) : null}
            </div>
            {!purchasable ? (
              <p className="text-sm text-sf-muted">
                Este producto no está a la venta ahora. Puedes mantenerlo o
                quitarlo de la lista.
              </p>
            ) : null}
          </>
        ) : (
          <>
            <h2 className="text-lg font-semibold text-sf-ink">
              Producto no disponible
            </h2>
            <p className="text-sm text-sf-muted">
              Ya no está en el catálogo o no se puede comprar ahora. Puedes
              quitarlo de la lista.
            </p>
            <Badge tone="danger" className="w-fit">
              No disponible
            </Badge>
          </>
        )}

        {removeError ? (
          <p className="text-sm text-sf-error">{removeError}</p>
        ) : null}
        {cartError ? (
          <p className="text-sm text-sf-error">{cartError}</p>
        ) : null}
        {addedFeedback ? (
          <p className="text-sm font-semibold text-sf-success" aria-live="polite">
            Añadido al carrito.
          </p>
        ) : null}

        <div className="mt-auto flex flex-col gap-3 md:flex-row md:flex-wrap md:items-center">
          <ListQuantityStepper
            shoppingListId={shoppingListId}
            productId={item.productId}
            quantity={item.quantity}
            disabled={busy || removeMutation.isPending}
          />
          {purchasable ? (
            <Button
              type="button"
              variant="secondary"
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
            disabled={busy || removeMutation.isPending}
            onClick={() => void removeMutation.mutateAsync(item.productId)}
          >
            {removeMutation.isPending ? "Eliminando…" : "Eliminar"}
          </Button>
        </div>
      </div>
    </Card>
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
