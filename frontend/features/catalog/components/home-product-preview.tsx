"use client";

import Link from "next/link";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import {
  ProductGrid,
  ProductGridSkeleton,
} from "@/features/catalog/components/product-grid";
import { useProductsQuery } from "@/features/catalog/hooks";
import { buttonClassName } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/empty-state";

const HOME_PRODUCT_LIMIT = 8;

export function HomeProductPreview() {
  const productsQuery = useProductsQuery();
  const products = productsQuery.data?.slice(0, HOME_PRODUCT_LIMIT) ?? [];

  return (
    <div>
      {productsQuery.isPending ? (
        <ProductGridSkeleton count={4} />
      ) : productsQuery.isError ? (
        <CatalogQueryError
          error={productsQuery.error}
          title="No se pudieron cargar los productos"
        />
      ) : products.length > 0 ? (
        <>
          <ProductGrid products={products} />
          <div className="mt-8">
            <Link href="/catalog" className={buttonClassName("secondary")}>
              Ver catálogo
            </Link>
          </div>
        </>
      ) : (
        <EmptyState
          title="No encontramos productos."
          description="Cuando haya productos en el súper, los verás aquí."
          action={
            <Link href="/catalog" className={buttonClassName("secondary")}>
              Ir al catálogo
            </Link>
          }
        />
      )}
    </div>
  );
}
