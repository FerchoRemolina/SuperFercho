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
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

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
                    onSuccess: () => {
                      closePanel();
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
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-28 w-full" />
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
        <ul className="grid gap-4">
          {typesQuery.data.map((productType) => (
            <li key={productType.id}>
              <ProductTypeCard
                productType={productType}
                onEdit={() => openEdit(productType)}
              />
            </li>
          ))}
        </ul>
      ) : null}
    </Card>
  );
}

function ProductTypeCard({
  productType,
  onEdit,
}: {
  productType: ProductType;
  onEdit: () => void;
}) {
  return (
    <div className="grid gap-3 rounded-xl border border-sf-border bg-sf-bg p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-lg font-semibold text-sf-ink">{productType.name}</p>
          <p className="mt-1 text-sm text-sf-muted">
            {productType.description?.trim()
              ? productType.description
              : "Sin descripción"}
          </p>
        </div>
        <ProductTypeStatusBadge status={productType.status} />
      </div>
      <div className="grid gap-2 sm:max-w-56">
        <Button type="button" variant="secondary" onClick={onEdit}>
          Editar
        </Button>
        <AdminProductTypeStatusActions productType={productType} compact />
      </div>
      <AdminProductVariantSection productTypeId={productType.id} />
    </div>
  );
}
