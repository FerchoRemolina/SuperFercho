"use client";

import { CatalogBrowse } from "@/features/catalog/components/catalog-browse";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import { useCategoryQuery } from "@/features/catalog/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function CategoryPageContent({ categoryId }: { categoryId: string }) {
  const categoryQuery = useCategoryQuery(categoryId);

  if (categoryQuery.isPending) {
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="mt-4 h-5 w-96 max-w-full" />
        <div className="mt-8 flex gap-2">
          <Skeleton className="h-11 w-20 rounded-full" />
          <Skeleton className="h-11 w-28 rounded-full" />
        </div>
        <div className="mt-8 grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 md:gap-5 lg:grid-cols-4">
          {[0, 1, 2, 3].map((index) => (
            <Skeleton key={index} className="aspect-square w-full rounded-2xl" />
          ))}
        </div>
      </Container>
    );
  }

  if (categoryQuery.isError) {
    const notFound =
      isApiError(categoryQuery.error) &&
      categoryQuery.error.problem.code === "CATEGORY_NOT_FOUND";
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        {notFound ? (
          <EmptyState
            title="No encontramos esta categoría."
            description="Puede que ya no esté disponible. Revisa el catálogo para ver otras opciones."
            align="center"
            className="py-14"
          />
        ) : (
          <CatalogQueryError
            error={categoryQuery.error}
            title="No se pudo cargar la categoría"
          />
        )}
      </Container>
    );
  }

  const category = categoryQuery.data;
  if (!category) {
    return null;
  }

  return (
    <CatalogBrowse
      title={category.name}
      description={category.description ?? undefined}
      categoryId={category.id}
      emptyTitle="No hay productos disponibles en esta categoría."
      emptyDescription="Cuando haya productos aquí, los verás en este listado."
    />
  );
}
