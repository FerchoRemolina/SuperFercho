"use client";

import { useEffect, useId, useRef, useState } from "react";
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
import { cx } from "@/shared/utils/cx";

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
  variant = "buttons",
}: {
  product: Product;
  compact?: boolean;
  /** buttons: botones en línea (detalle/mobile). menu: control ⋯ compacto. */
  variant?: "buttons" | "menu";
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

  if (variant === "menu") {
    return (
    <ProductStatusActionsMenu
      actions={actions}
      actionCopy={actionCopy}
      message={message}
      pending={pending}
      confirming={confirming}
      onOpenAction={(action) => {
        resetMutations();
        setConfirming(action);
      }}
      onCancelConfirm={() => setConfirming(null)}
      onRunConfirm={() => confirming && runAction(confirming)}
    />
    );
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

type StatusActionCopy = Record<
  ProductStatusAction,
  { label: string; title: string; body: string; confirm: string }
>;

/** Variante ⋯ compacta para filas de listado: reutiliza las mismas
 *  mutaciones, confirmaciones y manejo de errores que los botones. */
function ProductStatusActionsMenu({
  actions,
  actionCopy,
  message,
  pending,
  confirming,
  onOpenAction,
  onCancelConfirm,
  onRunConfirm,
}: {
  actions: ProductStatusAction[];
  actionCopy: StatusActionCopy;
  message: string | null;
  pending: boolean;
  confirming: ProductStatusAction | null;
  onOpenAction: (action: ProductStatusAction) => void;
  onCancelConfirm: () => void;
  onRunConfirm: () => void;
}) {
  const menuId = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }
    function onPointerDown(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [menuOpen]);

  return (
    <div className="relative flex items-center justify-end gap-1.5" ref={containerRef}>
      {message ? (
        <p className="absolute right-0 top-full mt-1 text-right text-xs text-sf-error">
          {message}
        </p>
      ) : null}
      {confirming === null ? (
        <>
          <button
            type="button"
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            aria-controls={menuId}
            aria-label="Más acciones del producto"
            disabled={pending}
            onClick={() => setMenuOpen((open) => !open)}
            className={cx(
              "inline-flex h-8 w-8 items-center justify-center rounded-lg border border-sf-border bg-sf-surface text-base font-bold leading-none text-sf-ink transition-colors hover:bg-sf-bg",
              "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
              "disabled:cursor-not-allowed disabled:opacity-60",
            )}
          >
            ⋯
          </button>
          {menuOpen ? (
          <div
            id={menuId}
            role="menu"
            aria-label="Más acciones del producto"
            className="absolute right-0 top-full z-20 mt-1 w-48 overflow-hidden rounded-xl border border-sf-border bg-sf-surface py-1 shadow-[0_8px_24px_rgba(16,24,40,0.10)]"
          >
            {actions.length === 0 ? (
              <p className="px-3 py-2 text-xs text-sf-muted">
                Sin acciones disponibles para este estado.
              </p>
            ) : (
              actions.map((action) => {
                const destructive =
                  action === "deactivate" || action === "archive";
                return (
                  <button
                    key={action}
                    type="button"
                    role="menuitem"
                    className={cx(
                      "flex w-full min-h-9 items-center px-3 text-left text-sm transition-colors hover:bg-sf-bg",
                      destructive ? "text-sf-error" : "text-sf-ink",
                    )}
                    onClick={() => {
                      setMenuOpen(false);
                      onOpenAction(action);
                    }}
                  >
                    {actionCopy[action].label}
                  </button>
                );
              })
            )}
          </div>
        ) : null}
        </>
      ) : (
        <div className="absolute right-0 top-full z-20 mt-1 w-64 rounded-xl border border-sf-border bg-sf-surface p-3 shadow-[0_8px_24px_rgba(16,24,40,0.10)]">
          <p className="text-sm font-semibold text-sf-ink">
            {actionCopy[confirming].title}
          </p>
          <p className="mt-1 text-xs leading-relaxed text-sf-muted">
            {actionCopy[confirming].body}
          </p>
          <div className="mt-3 flex items-center gap-2">
            <button
              type="button"
              disabled={pending}
              onClick={onRunConfirm}
              className={cx(
                "inline-flex min-h-8 flex-1 items-center justify-center rounded-lg px-3 text-sm font-semibold transition-colors",
                confirming === "deactivate" || confirming === "archive"
                  ? "bg-sf-error text-white hover:opacity-90"
                  : "bg-sf-primary text-white hover:bg-sf-primary-hover",
                "disabled:cursor-not-allowed disabled:opacity-60",
              )}
            >
              {pending ? "Guardando…" : actionCopy[confirming].confirm}
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={onCancelConfirm}
              className="inline-flex min-h-8 flex-1 items-center justify-center rounded-lg border border-sf-border bg-sf-surface px-3 text-sm font-semibold text-sf-ink transition-colors hover:bg-sf-bg disabled:cursor-not-allowed disabled:opacity-60"
            >
              Cancelar
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
