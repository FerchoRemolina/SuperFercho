"use client";

import Link from "next/link";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import { HomeProductCard } from "@/features/catalog/components/home-product-card";
import { ProductGridSkeleton } from "@/features/catalog/components/product-grid";
import { useBestSellersQuery } from "@/features/catalog/hooks";
import { buttonClassName } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/empty-state";

/**
 * "Los más vendidos": el backend resuelve ranking, ventana con fallback,
 * disponibilidad y diversidad; el preview solo renderiza el resultado.
 */
export function HomeProductPreview() {
  const bestSellersQuery = useBestSellersQuery();
  const products = bestSellersQuery.data ?? [];

  return (
    <div>
      {bestSellersQuery.isPending ? (
        <ProductGridSkeleton count={4} />
      ) : bestSellersQuery.isError ? (
        <CatalogQueryError
          error={bestSellersQuery.error}
          title="No se pudieron cargar los productos"
        />
      ) : products.length > 0 ? (
        <ul className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 md:gap-5 lg:grid-cols-4 lg:gap-5 xl:grid-cols-5">
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
          align="center"
          className="py-14"
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
