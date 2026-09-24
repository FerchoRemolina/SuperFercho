"use client";

import Link from "next/link";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import { HomeProductCard } from "@/features/catalog/components/home-product-card";
import { ProductGridSkeleton } from "@/features/catalog/components/product-grid";
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
        <ul className="grid grid-cols-2 gap-3 md:grid-cols-4 md:gap-6">
          {products.map((product) => (
            <li key={product.id}>
              <HomeProductCard product={product} />
            </li>
          ))}
        </ul>
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
