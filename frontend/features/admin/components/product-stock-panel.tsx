"use client";

import { useState, type FormEvent } from "react";
import {
  adjustAdminProductStockRequestFromValue,
  validateAdminProductStock,
} from "@/features/admin/payloads";
import { useAdjustAdminProductStockMutation } from "@/features/admin/hooks";
import type { Product } from "@/features/catalog/api";
import { productStockLabel } from "@/features/catalog/quantity";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { TextField } from "@/shared/ui/text-field";

export function ProductStockPanel({ product }: { product: Product }) {
  return (
    <Card className="grid gap-4">
      <div>
        <h2 className="text-xl font-semibold text-sf-ink">Stock</h2>
        <p className="mt-1 text-sm text-sf-muted">
          Stock actual:{" "}
          <span className="font-semibold text-sf-ink">
            {product.stock} · {productStockLabel(product.stock)}
          </span>
        </p>
      </div>
      <ProductStockForm
        key={`${product.id}-${product.updatedAt}-${product.stock}`}
        productId={product.id}
        initialStock={String(product.stock)}
      />
    </Card>
  );
}

function ProductStockForm({
  productId,
  initialStock,
}: {
  productId: string;
  initialStock: string;
}) {
  const mutation = useAdjustAdminProductStockMutation();
  const [stock, setStock] = useState(initialStock);
  const [fieldError, setFieldError] = useState<string | undefined>();
  const [saved, setSaved] = useState(false);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const error = validateAdminProductStock(stock);
    setFieldError(error);
    if (error) {
      return;
    }
    setSaved(false);
    mutation.mutate(
      {
        productId,
        body: adjustAdminProductStockRequestFromValue(stock),
      },
      {
        onSuccess: (updated) => {
          setStock(String(updated.stock));
          setSaved(true);
        },
      },
    );
  }

  const errorMessage = mutation.isError
    ? isApiError(mutation.error)
      ? messageForApiProblem(mutation.error.problem)
      : "No se pudo actualizar el stock."
    : null;

  return (
    <form className="grid gap-3" onSubmit={handleSubmit} noValidate>
      <TextField
        id={`stock-${productId}`}
        label="Nuevo stock"
        inputMode="numeric"
        value={stock}
        error={fieldError}
        onChange={(event) => {
          setSaved(false);
          setStock(event.target.value);
        }}
      />
      {errorMessage ? (
        <p className="text-sm text-sf-error">{errorMessage}</p>
      ) : null}
      {saved ? (
        <p className="text-sm font-semibold text-sf-success">
          Stock actualizado correctamente.
        </p>
      ) : null}
      <Button type="submit" variant="secondary" disabled={mutation.isPending}>
        {mutation.isPending ? "Guardando…" : "Ajustar stock"}
      </Button>
    </form>
  );
}
