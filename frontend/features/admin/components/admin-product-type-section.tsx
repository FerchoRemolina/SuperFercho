"use client";

import { useState } from "react";
import type { ProductType } from "@/features/admin/api";
import { AdminProductTypeForm } from "@/features/admin/components/admin-product-type-form";
import { AdminProductTypeStatusActions } from "@/features/admin/components/admin-product-type-status-actions";
import { AdminProductVariantSection } from "@/features/admin/components/admin-product-variant-section";
import { ProductTypeStatusBadge } from "@/features/admin/components/product-type-status-badge";
import {
  useAdminProductTypesQuery,
  useCreateAdminProductTypeMutation,
  useUpdateAdminProductTypeMutation,
} from "@/features/admin/hooks";
import {
  adminProductTypeFormValuesFromProductType,
  createAdminProductTypeRequestFromValues,
  emptyAdminProductTypeFormValues,
  updateAdminProductTypeRequestFromValues,
  validateAdminProductType,
  type AdminProductTypeFormValues,
} from "@/features/admin/payloads";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { ChevronIcon } from "@/shared/ui/icons";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

type Panel =
  | { kind: "create" }
  | { kind: "edit"; productType: ProductType }
  | null;

export function AdminProductTypeSection({
  categoryId,
}: {
  categoryId: string;
}) {
  const typesQuery = useAdminProductTypesQuery(categoryId);
  const createMutation = useCreateAdminProductTypeMutation();
  const updateMutation = useUpdateAdminProductTypeMutation();
  const [panel, setPanel] = useState<Panel>(null);
  const [formValues, setFormValues] = useState<AdminProductTypeFormValues>(
    emptyAdminProductTypeFormValues(),
  );
  // Acordeón: solo un tipo expandido a la vez. Los nuevos tipos quedan
  // expandidos automáticamente para facilitar su configuración.
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const formError =
    panel?.kind === "create" && createMutation.isError
      ? isApiError(createMutation.error)
        ? messageForApiProblem(createMutation.error.problem)
        : "No se pudo crear el tipo de producto."
      : panel?.kind === "edit" && updateMutation.isError
        ? isApiError(updateMutation.error)
          ? messageForApiProblem(updateMutation.error.problem)
          : "No se pudo guardar el tipo de producto."
        : null;

  const formPending =
    panel?.kind === "create"
      ? createMutation.isPending
      : panel?.kind === "edit"
        ? updateMutation.isPending
        : false;

  function openCreate() {
    createMutation.reset();
    updateMutation.reset();
    setFormValues(emptyAdminProductTypeFormValues());
    setPanel({ kind: "create" });
  }

  function openEdit(productType: ProductType) {
    createMutation.reset();
    updateMutation.reset();
    setFormValues(adminProductTypeFormValuesFromProductType(productType));
    setPanel({ kind: "edit", productType });
    setExpandedId(productType.id);
  }

  function closePanel() {
    setPanel(null);
    setFormValues(emptyAdminProductTypeFormValues());
  }

  return (
    <Card className="grid gap-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold text-sf-ink">
            Tipos de producto
          </h2>
          <p className="mt-1 text-sm text-sf-muted">
            Organiza los productos de esta categoría por tipo. El estado se
            cambia con activar o desactivar, no desde el formulario.
          </p>
        </div>
        {panel === null ? (
          <Button type="button" onClick={openCreate}>
            Crear tipo
          </Button>
        ) : null}
      </div>

      {panel !== null ? (
        <div className="grid gap-3 rounded-xl border border-sf-border bg-sf-bg p-4">
          <h3 className="text-base font-semibold text-sf-ink">
            {panel.kind === "create"
              ? "Nuevo tipo de producto"
              : `Editar: ${panel.productType.name}`}
          </h3>
          <AdminProductTypeForm
            mode={panel.kind === "create" ? "create" : "edit"}
            values={formValues}
            pending={formPending}
            error={formError}
            onChange={setFormValues}
            validate={validateAdminProductType}
            onCancel={closePanel}
            onSubmit={() => {
              if (panel.kind === "create") {
                createMutation.mutate(
                  createAdminProductTypeRequestFromValues(
                    categoryId,
                    formValues,
                  ),
                  {
                    onSuccess: (created) => {
                      closePanel();
                      setExpandedId(created.id);
                    },
                  },
                );
                return;
              }
              updateMutation.mutate(
                {
                  productTypeId: panel.productType.id,
                  body: updateAdminProductTypeRequestFromValues(formValues),
                },
                {
                  onSuccess: () => {
                    closePanel();
                  },
                },
              );
            }}
          />
        </div>
      ) : null}

      {typesQuery.isPending ? (
        <div className="grid gap-3" aria-hidden="true">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : null}

      {typesQuery.isError ? (
        <div className="grid gap-4">
          <Alert tone="error" title="No se pudieron cargar los tipos">
            {isApiError(typesQuery.error)
              ? messageForApiProblem(typesQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => typesQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      ) : null}

      {typesQuery.isSuccess && typesQuery.data.length === 0 && panel === null ? (
        <EmptyState
          title="No hay tipos de producto"
          description="Crea el primer tipo para organizar los productos de esta categoría."
          action={
            <Button type="button" onClick={openCreate}>
              Crear tipo
            </Button>
          }
        />
      ) : null}

      {typesQuery.isSuccess && typesQuery.data.length > 0 ? (
        <ul className="grid gap-3">
          {typesQuery.data.map((productType) => {
            const editing =
              panel?.kind === "edit" && panel.productType.id === productType.id;
            return (
              <li key={productType.id}>
                <ProductTypeCard
                  productType={productType}
                  expanded={expandedId === productType.id || editing}
                  onToggle={() =>
                    setExpandedId((current) =>
                      current === productType.id ? null : productType.id,
                    )
                  }
                  onEdit={() => openEdit(productType)}
                />
              </li>
            );
          })}
        </ul>
      ) : null}
    </Card>
  );
}

function ProductTypeCard({
  productType,
  expanded,
  onToggle,
  onEdit,
}: {
  productType: ProductType;
  expanded: boolean;
  onToggle: () => void;
  onEdit: () => void;
}) {
  return (
    <div
      className={cx(
        "overflow-hidden rounded-xl border bg-sf-surface",
        expanded ? "border-sf-primary/40" : "border-sf-border",
      )}
    >
      <button
        type="button"
        aria-expanded={expanded}
        onClick={onToggle}
        className={cx(
          "flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-sf-bg/60",
          "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
        )}
      >
        <ChevronIcon
          className={cx(
            "h-4 w-4 shrink-0 text-sf-muted transition-transform duration-200",
            expanded && "rotate-90",
          )}
        />
        <span className="min-w-0 flex-1">
          <span className="block truncate text-base font-semibold text-sf-ink">
            {productType.name}
          </span>
          {!expanded && productType.description?.trim() ? (
            <span className="block truncate text-xs text-sf-muted">
              {productType.description}
            </span>
          ) : null}
        </span>
        <ProductTypeStatusBadge status={productType.status} />
      </button>
      {expanded ? (
        <div className="grid gap-3 border-t border-sf-border px-4 py-4">
          <p className="text-sm text-sf-muted">
            {productType.description?.trim()
              ? productType.description
              : "Sin descripción"}
          </p>
          <div className="flex flex-wrap items-start gap-2">
            <Button type="button" variant="secondary" onClick={onEdit}>
              Editar
            </Button>
            <div className="sm:max-w-56">
              <AdminProductTypeStatusActions productType={productType} compact />
            </div>
          </div>
          <AdminProductVariantSection productTypeId={productType.id} />
        </div>
      ) : null}
    </div>
  );
}
