"use client";

import Link from "next/link";
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
    <Container as="main" className="py-8 md:py-16">
      <Link
        href="/catalog"
        className="mb-3 inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:text-sf-primary md:mb-4"
      >
        ← Catálogo
      </Link>
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Buscar
      </h1>
      {text ? (
        <p className="mt-2 text-sm text-sf-muted md:text-base">
          Resultados para “{text}”
        </p>
      ) : (
        <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
          Encuentra productos por nombre, marca o código.
        </p>
      )}

      <div className="mt-5 md:mt-6">
        <SearchBar key={text} initialText={text} autoFocus={!text} />
      </div>

      <div className="mt-6 md:mt-8">
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
