"use client";

import { CategoryChips } from "@/features/catalog/components/category-chips";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import {
  ProductGrid,
  ProductGridSkeleton,
} from "@/features/catalog/components/product-grid";
import { useCategoriesQuery, useProductsQuery } from "@/features/catalog/hooks";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function CatalogBrowse({
  title,
  description,
  categoryId,
  emptyTitle,
  emptyDescription,
}: {
  title: string;
  description?: string;
  categoryId?: string;
  emptyTitle: string;
  emptyDescription: string;
}) {
  const categoriesQuery = useCategoriesQuery();
  const productsQuery = useProductsQuery(categoryId);

  return (
    <Container as="main" className="py-10 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        {title}
      </h1>
      {description ? (
        <p className="mt-3 max-w-2xl text-base text-sf-muted">{description}</p>
      ) : null}

      <div className="mt-8">
        {categoriesQuery.isPending ? (
          <div className="flex flex-wrap gap-2" aria-hidden="true">
            <Skeleton className="h-11 w-20" />
            <Skeleton className="h-11 w-28" />
            <Skeleton className="h-11 w-24" />
          </div>
        ) : categoriesQuery.isError ? (
          <CatalogQueryError error={categoriesQuery.error} title="No se pudieron cargar las categorías" />
        ) : categoriesQuery.data && categoriesQuery.data.length > 0 ? (
          <CategoryChips
            categories={categoriesQuery.data}
            activeCategoryId={categoryId}
          />
        ) : null}
      </div>

      <div className="mt-8">
        {productsQuery.isPending ? (
          <ProductGridSkeleton />
        ) : productsQuery.isError ? (
          <CatalogQueryError error={productsQuery.error} title="No se pudieron cargar los productos" />
        ) : productsQuery.data && productsQuery.data.length > 0 ? (
          <ProductGrid products={productsQuery.data} />
        ) : (
          <EmptyState title={emptyTitle} description={emptyDescription} />
        )}
      </div>
    </Container>
  );
}
