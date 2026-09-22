"use client";

import { useState, type FormEvent } from "react";
import {
  changeAdminProductPriceRequestFromAmount,
  validateAdminProductPrice,
} from "@/features/admin/payloads";
import { useChangeAdminProductPriceMutation } from "@/features/admin/hooks";
import type { Product } from "@/features/catalog/api";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { TextField } from "@/shared/ui/text-field";

export function ProductPricePanel({ product }: { product: Product }) {
  return (
    <Card className="grid gap-4">
      <div>
        <h2 className="text-xl font-semibold text-sf-ink">Precio</h2>
        <p className="mt-1 text-sm text-sf-muted">
          Precio actual:{" "}
          <span className="font-semibold text-sf-ink">
            {formatMoney(product.price)}
          </span>
        </p>
      </div>
      <ProductPriceForm
        key={`${product.id}-${product.updatedAt}`}
        productId={product.id}
        initialPrice={String(product.price.amount)}
      />
    </Card>
  );
}

function ProductPriceForm({
  productId,
  initialPrice,
}: {
  productId: string;
  initialPrice: string;
}) {
  const mutation = useChangeAdminProductPriceMutation();
  const [price, setPrice] = useState(initialPrice);
  const [fieldError, setFieldError] = useState<string | undefined>();
  const [saved, setSaved] = useState(false);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const error = validateAdminProductPrice(price);
    setFieldError(error);
    if (error) {
      return;
    }
    setSaved(false);
    mutation.mutate(
      {
        productId,
        body: changeAdminProductPriceRequestFromAmount(price),
      },
      {
        onSuccess: (updated) => {
          setPrice(String(updated.price.amount));
          setSaved(true);
        },
      },
    );
  }

  const errorMessage = mutation.isError
    ? isApiError(mutation.error)
      ? messageForApiProblem(mutation.error.problem)
      : "No se pudo actualizar el precio."
    : null;

  return (
    <form className="grid gap-3" onSubmit={handleSubmit} noValidate>
      <TextField
        id={`price-${productId}`}
        label="Nuevo precio (COP)"
        inputMode="decimal"
        value={price}
        error={fieldError}
        onChange={(event) => {
          setSaved(false);
          setPrice(event.target.value);
        }}
      />
      {errorMessage ? (
        <p className="text-sm text-sf-error">{errorMessage}</p>
      ) : null}
      {saved ? (
        <p className="text-sm font-semibold text-sf-success">
          Precio actualizado correctamente.
        </p>
      ) : null}
      <Button type="submit" variant="secondary" disabled={mutation.isPending}>
        {mutation.isPending ? "Guardando…" : "Cambiar precio"}
      </Button>
    </form>
  );
}
