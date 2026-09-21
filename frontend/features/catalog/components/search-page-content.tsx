"use client";

import { useSearchParams } from "next/navigation";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import {
  ProductGrid,
  ProductGridSkeleton,
} from "@/features/catalog/components/product-grid";
import { SearchBar } from "@/features/catalog/components/search-bar";
import { useSearchProductsQuery } from "@/features/catalog/hooks";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";

export function SearchPageContent() {
  const searchParams = useSearchParams();
  const text = (searchParams.get("text") ?? "").trim();
  const searchQuery = useSearchProductsQuery(text);

  return (
    <Container as="main" className="py-10 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Buscar
      </h1>
      <p className="mt-3 mb-8 max-w-2xl text-base text-sf-muted">
        Encuentra productos por nombre, marca o código.
      </p>
      <SearchBar key={text} initialText={text} autoFocus={!text} />

      <div className="mt-8">
        {!text ? (
          <EmptyState
            title="Escribe para buscar"
            description="Usa el nombre, la marca o el código del producto."
          />
        ) : searchQuery.isPending ? (
          <ProductGridSkeleton />
        ) : searchQuery.isError ? (
          <CatalogQueryError
            error={searchQuery.error}
            title="No se pudo buscar"
          />
        ) : searchQuery.data && searchQuery.data.length > 0 ? (
          <ProductGrid products={searchQuery.data} />
        ) : (
          <EmptyState
            title="No encontramos productos."
            description={`No hay resultados para “${text}”. Prueba con otro término.`}
          />
        )}
      </div>
    </Container>
  );
}
