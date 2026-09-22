"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAddCartItemMutation } from "@/features/cart/hooks";
import { savePendingAddToCart } from "@/features/cart/pending-intent";
import { ProductQuantitySelector } from "@/features/catalog/components/product-quantity-selector";
import {
  clampSelectedQuantity,
  initialSelectedQuantity,
  quantityToAddToCart,
} from "@/features/catalog/quantity";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { useSession } from "@/shared/session/session-provider";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { cx } from "@/shared/utils/cx";

export function AddToCartButton({
  productId,
  available,
  stock,
  className,
}: {
  productId: string;
  available: boolean;
  stock?: number;
  className?: string;
}) {
  const { session } = useSession();
  const router = useRouter();
  const addMutation = useAddCartItemMutation();
  const [added, setAdded] = useState(false);
  const [quantity, setQuantity] = useState(() =>
    stock === undefined ? 1 : initialSelectedQuantity(stock),
  );
  const showSelector = stock !== undefined && stock > 0;
  const selectedQuantity =
    stock === undefined ? quantity : clampSelectedQuantity(quantity, stock);

  if (showSelector && selectedQuantity !== quantity) {
    setQuantity(selectedQuantity);
  }

  if (!available || session?.role === "ADMIN") {
    return null;
  }

  async function onAdd() {
    const nextQuantity = quantityToAddToCart(selectedQuantity, stock);
    if (nextQuantity === null) {
      return;
    }
    if (!session) {
      savePendingAddToCart({ productId, quantity: nextQuantity });
      router.push(`/login?next=${encodeURIComponent("/cart")}`);
      return;
    }

    try {
      await addMutation.mutateAsync({ productId, quantity: nextQuantity });
      setAdded(true);
      window.setTimeout(() => setAdded(false), 2000);
    } catch {
      setAdded(false);
    }
  }

  const errorMessage =
    addMutation.isError && isApiError(addMutation.error)
      ? messageForApiProblem(addMutation.error.problem)
      : addMutation.isError
        ? "No se pudo añadir al carrito."
        : null;

  return (
    <div className={cx("grid gap-2", className)}>
      {errorMessage ? (
        <Alert tone="error" title="No se pudo añadir">
          {errorMessage}
        </Alert>
      ) : null}
      {showSelector && stock !== undefined ? (
        <ProductQuantitySelector
          quantity={selectedQuantity}
          stock={stock}
          onChange={setQuantity}
          disabled={addMutation.isPending}
        />
      ) : null}
      <Button
        type="button"
        onClick={onAdd}
        disabled={addMutation.isPending}
        className="w-full"
      >
        {addMutation.isPending
          ? "Añadiendo…"
          : added
            ? "Añadido"
            : "Añadir al carrito"}
      </Button>
      <p className="sr-only" aria-live="polite">
        {added ? "Producto añadido al carrito." : ""}
      </p>
    </div>
  );
}
