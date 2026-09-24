"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { AdminCategoryForm } from "@/features/admin/components/admin-category-form";
import { AdminProductTypeSection } from "@/features/admin/components/admin-product-type-section";
import { CategoryStatusActions } from "@/features/admin/components/category-status-actions";
import { CategoryStatusBadge } from "@/features/admin/components/category-status-badge";
import {
  useAdminCategoryQuery,
  useUpdateAdminCategoryMutation,
} from "@/features/admin/hooks";
import {
  adminCategoryFormValuesFromCategory,
  type AdminCategoryFormValues,
  updateAdminCategoryRequestFromValues,
  validateAdminCategory,
} from "@/features/admin/payloads";
import { formatAdminInstant } from "@/features/admin/presentation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminCategoryDetailPageContent({
  categoryId,
}: {
  categoryId: string;
}) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const created = searchParams.get("created") === "1";
  const categoryQuery = useAdminCategoryQuery(categoryId);
  const updateMutation = useUpdateAdminCategoryMutation();
  const [draftValues, setDraftValues] = useState<AdminCategoryFormValues | null>(
    null,
  );
  const [saved, setSaved] = useState(false);

  const category = categoryQuery.data;
  const values = useMemo(
    () =>
      draftValues ??
      (category ? adminCategoryFormValuesFromCategory(category) : null),
    [category, draftValues],
  );

  const updateError = updateMutation.isError
    ? isApiError(updateMutation.error)
      ? messageForApiProblem(updateMutation.error.problem)
      : "No se pudo guardar la categoría."
    : null;

  if (categoryQuery.isPending) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-96 w-full" />
      </Container>
    );
  }

  if (categoryQuery.isError) {
    const notFound =
      isApiError(categoryQuery.error) &&
      categoryQuery.error.problem.code === "CATEGORY_NOT_FOUND";
    return (
      <Container as="main" className="py-10 md:py-16">
        {notFound ? (
          <EmptyState
            title="No encontramos esta categoría"
            description="Puede que el identificador no exista en el catálogo."
            action={
              <Link
                href="/admin/categories"
                className={buttonClassName("secondary")}
              >
                Volver al listado
              </Link>
            }
          />
        ) : (
          <Alert tone="error" title="No se pudo cargar la categoría">
            {messageForApiProblem(
              isApiError(categoryQuery.error)
                ? categoryQuery.error.problem
                : { status: 500 },
            )}
          </Alert>
        )}
      </Container>
    );
  }

  if (!category || !values) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-96 w-full" />
      </Container>
    );
  }

  return (
    <Container as="main" className="py-10 md:py-16">
      <Link
        href="/admin/categories"
        className={`${buttonClassName("ghost")} mb-4 px-0`}
      >
        Volver al listado
      </Link>

      {created ? (
        <div className="mb-6">
          <Alert tone="success" title="Categoría creada">
            Ya puedes editar la ficha o cambiar su estado.
          </Alert>
        </div>
      ) : null}

      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            {category.name}
          </h1>
          <p className="mt-2 text-base text-sf-muted">
            {category.description?.trim()
              ? category.description
              : "Sin descripción"}
          </p>
        </div>
        <CategoryStatusBadge status={category.status} />
      </div>

      <div className="mt-8 grid gap-6">
        <Card className="grid gap-3 text-sm">
          <h2 className="text-lg font-semibold text-sf-ink">
            Información actual
          </h2>
          <dl className="grid gap-2 sm:grid-cols-2">
            <div>
              <dt className="text-sf-muted">Creada</dt>
              <dd className="text-sf-ink">
                {formatAdminInstant(category.createdAt)}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Actualizada</dt>
              <dd className="text-sf-ink">
                {formatAdminInstant(category.updatedAt)}
              </dd>
            </div>
          </dl>
        </Card>

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Ficha</h2>
          {saved ? (
            <p className="text-sm font-semibold text-sf-success">
              Cambios guardados correctamente.
            </p>
          ) : null}
          <AdminCategoryForm
            mode="edit"
            values={values}
            pending={updateMutation.isPending}
            error={updateError}
            onChange={setDraftValues}
            validate={validateAdminCategory}
            onCancel={() => setDraftValues(null)}
            onSubmit={() => {
              setSaved(false);
              updateMutation.mutate(
                {
                  categoryId: category.id,
                  body: updateAdminCategoryRequestFromValues(values),
                },
                {
                  onSuccess: () => {
                    setDraftValues(null);
                    setSaved(true);
                  },
                },
              );
            }}
          />
        </Card>

        <Card className="grid gap-3">
          <h2 className="text-xl font-semibold text-sf-ink">Estado</h2>
          <CategoryStatusActions category={category} />
        </Card>

        <AdminProductTypeSection categoryId={category.id} />
      </div>

      <div className="mt-8">
        <Button
          type="button"
          variant="secondary"
          onClick={() => router.push("/admin/categories")}
        >
          Volver al listado
        </Button>
      </div>
    </Container>
  );
}
