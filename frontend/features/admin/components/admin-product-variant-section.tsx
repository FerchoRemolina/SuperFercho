"use client";

import { useState } from "react";
import type { ProductVariant } from "@/features/admin/api";
import { AdminProductVariantForm } from "@/features/admin/components/admin-product-variant-form";
import { AdminProductVariantStatusActions } from "@/features/admin/components/admin-product-variant-status-actions";
import { ProductVariantStatusBadge } from "@/features/admin/components/product-variant-status-badge";
import {
  useAdminProductVariantsQuery,
  useCreateAdminProductVariantMutation,
  useUpdateAdminProductVariantMutation,
} from "@/features/admin/hooks";
import {
  adminProductVariantFormValuesFromProductVariant,
  createAdminProductVariantRequestFromValues,
  emptyAdminProductVariantFormValues,
  updateAdminProductVariantRequestFromValues,
  validateAdminProductVariant,
  type AdminProductVariantFormValues,
} from "@/features/admin/payloads";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

type Panel =
  | { kind: "create" }
  | { kind: "edit"; productVariant: ProductVariant }
  | null;

export function AdminProductVariantSection({
  productTypeId,
}: {
  productTypeId: string;
}) {
  const variantsQuery = useAdminProductVariantsQuery(productTypeId);
  const createMutation = useCreateAdminProductVariantMutation();
  const updateMutation = useUpdateAdminProductVariantMutation();
  const [panel, setPanel] = useState<Panel>(null);
  const [formValues, setFormValues] = useState<AdminProductVariantFormValues>(
    emptyAdminProductVariantFormValues(),
  );

  const formError =
    panel?.kind === "create" && createMutation.isError
      ? isApiError(createMutation.error)
        ? messageForApiProblem(createMutation.error.problem)
        : "No se pudo crear la variante."
      : panel?.kind === "edit" && updateMutation.isError
        ? isApiError(updateMutation.error)
          ? messageForApiProblem(updateMutation.error.problem)
          : "No se pudo guardar la variante."
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
    setFormValues(emptyAdminProductVariantFormValues());
    setPanel({ kind: "create" });
  }

  function openEdit(productVariant: ProductVariant) {
    createMutation.reset();
    updateMutation.reset();
    setFormValues(
      adminProductVariantFormValuesFromProductVariant(productVariant),
    );
    setPanel({ kind: "edit", productVariant });
  }

  function closePanel() {
    setPanel(null);
    setFormValues(emptyAdminProductVariantFormValues());
  }

  return (
    <div className="grid gap-3 border-t border-sf-border pt-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h4 className="text-base font-semibold text-sf-ink">Variantes</h4>
          <p className="mt-1 text-sm text-sf-muted">
            Opcionales. El estado se cambia con activar o desactivar, no desde
            el formulario.
          </p>
        </div>
        {panel === null ? (
          <Button type="button" variant="secondary" onClick={openCreate}>
            Nueva variante
          </Button>
        ) : null}
      </div>

      {panel !== null ? (
        <div className="grid gap-3 rounded-xl border border-sf-border bg-sf-surface p-4">
          <h5 className="text-sm font-semibold text-sf-ink">
            {panel.kind === "create"
              ? "Nueva variante"
              : `Editar: ${panel.productVariant.name}`}
          </h5>
          <AdminProductVariantForm
            mode={panel.kind === "create" ? "create" : "edit"}
            values={formValues}
            pending={formPending}
            error={formError}
            onChange={setFormValues}
            validate={validateAdminProductVariant}
            onCancel={closePanel}
            onSubmit={() => {
              if (panel.kind === "create") {
                createMutation.mutate(
                  createAdminProductVariantRequestFromValues(
                    productTypeId,
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
                  productVariantId: panel.productVariant.id,
                  body: updateAdminProductVariantRequestFromValues(formValues),
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

      {variantsQuery.isPending ? (
        <div className="grid gap-3" aria-hidden="true">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      ) : null}

      {variantsQuery.isError ? (
        <div className="grid gap-4">
          <Alert tone="error" title="No se pudieron cargar las variantes">
            {isApiError(variantsQuery.error)
              ? messageForApiProblem(variantsQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => variantsQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      ) : null}

      {variantsQuery.isSuccess &&
      variantsQuery.data.length === 0 &&
      panel === null ? (
        <EmptyState
          title="No hay variantes"
          description="Las variantes son opcionales. Puedes crear una si este tipo las necesita."
          action={
            <Button type="button" variant="secondary" onClick={openCreate}>
              Nueva variante
            </Button>
          }
        />
      ) : null}

      {variantsQuery.isSuccess && variantsQuery.data.length > 0 ? (
        <>
          <ul className="grid gap-3 md:hidden">
            {variantsQuery.data.map((productVariant) => (
              <li key={productVariant.id}>
                <ProductVariantCard
                  productVariant={productVariant}
                  onEdit={() => openEdit(productVariant)}
                />
              </li>
            ))}
          </ul>

          <div className="hidden overflow-x-auto md:block">
            <table className="w-full min-w-[28rem] border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-sf-border text-sf-muted">
                  <th className="px-3 py-3 font-semibold">Nombre</th>
                  <th className="px-3 py-3 font-semibold">Descripción</th>
                  <th className="px-3 py-3 font-semibold">Estado</th>
                  <th className="px-3 py-3 font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {variantsQuery.data.map((productVariant) => (
                  <tr
                    key={productVariant.id}
                    className="border-b border-sf-border align-top"
                  >
                    <td className="px-3 py-4 font-semibold text-sf-ink">
                      {productVariant.name}
                    </td>
                    <td className="px-3 py-4 text-sf-muted">
                      {productVariant.description?.trim()
                        ? productVariant.description
                        : "—"}
                    </td>
                    <td className="px-3 py-4">
                      <ProductVariantStatusBadge
                        status={productVariant.status}
                      />
                    </td>
                    <td className="px-3 py-4">
                      <div className="grid max-w-56 gap-2">
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={() => openEdit(productVariant)}
                        >
                          Editar
                        </Button>
                        <AdminProductVariantStatusActions
                          productVariant={productVariant}
                          compact
                        />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </div>
  );
}

function ProductVariantCard({
  productVariant,
  onEdit,
}: {
  productVariant: ProductVariant;
  onEdit: () => void;
}) {
  return (
    <div className="grid gap-3 rounded-xl border border-sf-border bg-sf-surface p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-base font-semibold text-sf-ink">
            {productVariant.name}
          </p>
          <p className="mt-1 text-sm text-sf-muted">
            {productVariant.description?.trim()
              ? productVariant.description
              : "Sin descripción"}
          </p>
        </div>
        <ProductVariantStatusBadge status={productVariant.status} />
      </div>
      <div className="grid gap-2">
        <Button type="button" variant="secondary" onClick={onEdit}>
          Editar
        </Button>
        <AdminProductVariantStatusActions
          productVariant={productVariant}
          compact
        />
      </div>
    </div>
  );
}
