"use client";

import { useState } from "react";
import type { ProductVariant } from "@/features/admin/api";
import {
  useActivateAdminProductVariantMutation,
  useDeactivateAdminProductVariantMutation,
} from "@/features/admin/hooks";
import { productVariantStatusLabel } from "@/features/admin/presentation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Button } from "@/shared/ui/button";

export function AdminProductVariantStatusActions({
  productVariant,
  compact = false,
}: {
  productVariant: ProductVariant;
  compact?: boolean;
}) {
  const activate = useActivateAdminProductVariantMutation();
  const deactivate = useDeactivateAdminProductVariantMutation();
  const [confirming, setConfirming] = useState<"activate" | "deactivate" | null>(
    null,
  );
  const pending = activate.isPending || deactivate.isPending;
  const error = activate.error ?? deactivate.error;
  const message = error
    ? isApiError(error)
      ? messageForApiProblem(error.problem)
      : "No se pudo completar la solicitud."
    : null;

  const next = productVariant.status === "ACTIVE" ? "deactivate" : "activate";

  return (
    <div className="grid gap-2">
      {message ? <p className="text-sm text-sf-error">{message}</p> : null}
      {confirming === null ? (
        <Button
          type="button"
          variant={next === "deactivate" ? "secondary" : "primary"}
          className={compact ? "w-full" : undefined}
          disabled={pending}
          onClick={() => {
            activate.reset();
            deactivate.reset();
            setConfirming(next);
          }}
        >
          {next === "deactivate" ? "Desactivar" : "Activar"}
        </Button>
      ) : (
        <div className="grid gap-2 rounded-xl border border-sf-border bg-sf-bg p-3">
          <p className="text-sm font-semibold text-sf-ink">
            {confirming === "deactivate"
              ? "¿Desactivar esta variante?"
              : "¿Activar esta variante?"}
          </p>
          <p className="text-sm text-sf-muted">
            {confirming === "deactivate"
              ? "Los productos con esta variante dejarán de venderse mientras la variante esté inactiva."
              : `La variante pasará a estado ${productVariantStatusLabel("ACTIVE")}.`}
          </p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button
              type="button"
              variant={confirming === "deactivate" ? "destructive" : "primary"}
              disabled={pending}
              onClick={() => {
                const mutation =
                  confirming === "deactivate" ? deactivate : activate;
                mutation.mutate(productVariant.id, {
                  onSuccess: () => setConfirming(null),
                });
              }}
            >
              {pending
                ? "Guardando…"
                : confirming === "deactivate"
                  ? "Sí, desactivar"
                  : "Sí, activar"}
            </Button>
            <Button
              type="button"
              variant="secondary"
              disabled={pending}
              onClick={() => setConfirming(null)}
            >
              Cancelar
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
