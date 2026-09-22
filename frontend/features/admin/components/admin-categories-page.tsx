"use client";

import Link from "next/link";
import { CategoryStatusActions } from "@/features/admin/components/category-status-actions";
import { CategoryStatusBadge } from "@/features/admin/components/category-status-badge";
import { useAdminCategoriesQuery } from "@/features/admin/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminCategoriesPageContent() {
  const categoriesQuery = useAdminCategoriesQuery();

  return (
    <Container as="main" className="py-10 md:py-16">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            Categorías
          </h1>
          <p className="mt-2 max-w-2xl text-base text-sf-muted">
            Gestiona las categorías del catálogo. El estado se cambia con
            activar o desactivar, no desde el formulario de edición.
          </p>
        </div>
        <Link
          href="/admin/categories/new"
          className={buttonClassName("primary")}
        >
          Crear categoría
        </Link>
      </div>

      {categoriesQuery.isPending ? (
        <div className="mt-8 grid gap-3" aria-hidden="true">
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-28 w-full" />
        </div>
      ) : null}

      {categoriesQuery.isError ? (
        <div className="mt-8 grid gap-4">
          <Alert tone="error" title="No se pudieron cargar las categorías">
            {isApiError(categoriesQuery.error)
              ? messageForApiProblem(categoriesQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => categoriesQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      ) : null}

      {categoriesQuery.isSuccess && categoriesQuery.data.length === 0 ? (
        <div className="mt-8">
          <EmptyState
            title="No hay categorías"
            description="Crea la primera categoría para organizar el catálogo."
            action={
              <Link
                href="/admin/categories/new"
                className={buttonClassName("primary")}
              >
                Crear categoría
              </Link>
            }
          />
        </div>
      ) : null}

      {categoriesQuery.isSuccess && categoriesQuery.data.length > 0 ? (
        <>
          <ul className="mt-8 grid gap-4 md:hidden">
            {categoriesQuery.data.map((category) => (
              <li key={category.id}>
                <Card className="grid gap-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-lg font-semibold text-sf-ink">
                        {category.name}
                      </p>
                      <p className="mt-1 text-sm text-sf-muted">
                        {category.description?.trim()
                          ? category.description
                          : "Sin descripción"}
                      </p>
                    </div>
                    <CategoryStatusBadge status={category.status} />
                  </div>
                  <div className="grid gap-2">
                    <Link
                      href={`/admin/categories/${category.id}`}
                      className={buttonClassName("secondary")}
                    >
                      Ver y editar
                    </Link>
                    <CategoryStatusActions category={category} compact />
                  </div>
                </Card>
              </li>
            ))}
          </ul>

          <div className="mt-8 hidden overflow-x-auto md:block">
            <table className="w-full min-w-[40rem] border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-sf-border text-sf-muted">
                  <th className="px-3 py-3 font-semibold">Nombre</th>
                  <th className="px-3 py-3 font-semibold">Descripción</th>
                  <th className="px-3 py-3 font-semibold">Estado</th>
                  <th className="px-3 py-3 font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {categoriesQuery.data.map((category) => (
                  <tr
                    key={category.id}
                    className="border-b border-sf-border align-top"
                  >
                    <td className="px-3 py-4 font-semibold text-sf-ink">
                      {category.name}
                    </td>
                    <td className="px-3 py-4 text-sf-muted">
                      {category.description?.trim()
                        ? category.description
                        : "—"}
                    </td>
                    <td className="px-3 py-4">
                      <CategoryStatusBadge status={category.status} />
                    </td>
                    <td className="px-3 py-4">
                      <div className="grid max-w-56 gap-2">
                        <Link
                          href={`/admin/categories/${category.id}`}
                          className={buttonClassName("secondary")}
                        >
                          Ver y editar
                        </Link>
                        <CategoryStatusActions category={category} compact />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </Container>
  );
}
