"use client";

import { useState } from "react";
import {
  useActivateAdminProductMutation,
  useArchiveAdminProductMutation,
  useDeactivateAdminProductMutation,
  useRestoreAdminProductMutation,
} from "@/features/admin/hooks";
import {
  canActivateProduct,
  canArchiveProduct,
  canDeactivateProduct,
  canRestoreProduct,
  productStatusLabel,
} from "@/features/admin/presentation";
import type { Product } from "@/features/catalog/api";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Button } from "@/shared/ui/button";

type ProductStatusAction = "activate" | "deactivate" | "archive" | "restore";

const actionCopy: Record<
  ProductStatusAction,
  { label: string; title: string; body: string; confirm: string }
> = {
  activate: {
    label: "Activar",
    title: "¿Activar este producto?",
    body: `El producto pasará a estado ${productStatusLabel("ACTIVE")} y podrá venderse si hay stock.`,
    confirm: "Sí, activar",
  },
  deactivate: {
    label: "Desactivar",
    title: "¿Desactivar este producto?",
    body: "Dejará de verse en el catálogo público y no se podrá vender.",
    confirm: "Sí, desactivar",
  },
  archive: {
    label: "Archivar",
    title: "¿Archivar este producto?",
    body: "El producto pasará a Archivado, su stock se establecerá en 0 y dejará de estar disponible en el catálogo. No se elimina: podrás restaurarlo después a Inactivo.",
    confirm: "Sí, archivar",
  },
  restore: {
    label: "Restaurar",
    title: "¿Restaurar este producto?",
    body: `Pasará a ${productStatusLabel("INACTIVE")} con stock 0. Luego podrás ajustar el stock y activarlo para volver a venderlo.`,
    confirm: "Sí, restaurar",
  },
};

export function ProductStatusActions({
  product,
  compact = false,
}: {
  product: Product;
  compact?: boolean;
}) {
  const activate = useActivateAdminProductMutation();
  const deactivate = useDeactivateAdminProductMutation();
  const archive = useArchiveAdminProductMutation();
  const restore = useRestoreAdminProductMutation();
  const [confirming, setConfirming] = useState<ProductStatusAction | null>(null);
  const pending =
    activate.isPending ||
    deactivate.isPending ||
    archive.isPending ||
    restore.isPending;
  const error =
    activate.error ?? deactivate.error ?? archive.error ?? restore.error;
  const message = error
    ? isApiError(error)
      ? messageForApiProblem(error.problem)
      : "No se pudo completar la solicitud."
    : null;

  const actions: ProductStatusAction[] = [];
  if (canDeactivateProduct(product.status)) {
    actions.push("deactivate");
  }
  if (canActivateProduct(product.status)) {
    actions.push("activate");
  }
  if (canArchiveProduct(product.status)) {
    actions.push("archive");
  }
  if (canRestoreProduct(product.status)) {
    actions.push("restore");
  }

  function resetMutations() {
    activate.reset();
    deactivate.reset();
    archive.reset();
    restore.reset();
  }

  function runAction(action: ProductStatusAction) {
    const mutation =
      action === "activate"
        ? activate
        : action === "deactivate"
          ? deactivate
          : action === "archive"
            ? archive
            : restore;
    mutation.mutate(product.id, {
      onSuccess: () => setConfirming(null),
    });
  }

  return (
    <div className="grid gap-2">
      {message ? <p className="text-sm text-sf-error">{message}</p> : null}
      {confirming === null ? (
        <div className={compact ? "grid gap-2" : "flex flex-wrap gap-2"}>
          {actions.map((action) => (
            <Button
              key={action}
              type="button"
              variant={
                action === "deactivate" || action === "archive"
                  ? "secondary"
                  : "primary"
              }
              className={compact ? "w-full" : undefined}
              disabled={pending}
              onClick={() => {
                resetMutations();
                setConfirming(action);
              }}
            >
              {actionCopy[action].label}
            </Button>
          ))}
        </div>
      ) : (
        <div className="grid gap-2 rounded-xl border border-sf-border bg-sf-bg p-3">
          <p className="text-sm font-semibold text-sf-ink">
            {actionCopy[confirming].title}
          </p>
          <p className="text-sm text-sf-muted">{actionCopy[confirming].body}</p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button
              type="button"
              variant={
                confirming === "deactivate" || confirming === "archive"
                  ? "destructive"
                  : "primary"
              }
              disabled={pending}
              onClick={() => runAction(confirming)}
            >
              {pending ? "Guardando…" : actionCopy[confirming].confirm}
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
