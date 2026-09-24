"use client";

import Link from "next/link";
import { useProductsQuery } from "@/features/catalog/hooks";
import { FavoriteItemCard } from "@/features/favorites/components/favorite-item-card";
import { useFavoritesQuery } from "@/features/favorites/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function FavoritesPageContent() {
  const favoritesQuery = useFavoritesQuery();
  const productsQuery = useProductsQuery();

  if (favoritesQuery.isPending) {
    return (
      <Container as="main" className="py-8 md:py-16">
        <FavoritesHeading />
        <FavoritesSkeleton />
      </Container>
    );
  }

  if (favoritesQuery.isError) {
    const message = isApiError(favoritesQuery.error)
      ? messageForApiProblem(favoritesQuery.error.problem)
      : "No se pudieron cargar tus favoritos.";
    return (
      <Container as="main" className="py-8 md:py-16">
        <FavoritesHeading />
        <div className="mt-6 grid gap-4 md:mt-8">
          <Alert tone="error" title="No se pudieron cargar los favoritos">
            {message}
          </Alert>
          <Button type="button" variant="secondary" onClick={() => favoritesQuery.refetch()}>
            Reintentar
          </Button>
        </div>
      </Container>
    );
  }

  const items = favoritesQuery.data?.items ?? [];
  if (items.length === 0) {
    return (
      <Container as="main" className="py-8 md:py-16">
        <FavoritesHeading />
        <div className="mt-6 md:mt-8">
          <EmptyState
            title="Todavía no tienes favoritos"
            description="Marca productos del catálogo para encontrarlos aquí más rápido."
            action={
              <Link href="/catalog" className={buttonClassName("primary")}>
                Ir al catálogo
              </Link>
            }
          />
        </div>
      </Container>
    );
  }

  const productsById = new Map(
    (productsQuery.data ?? []).map((product) => [product.id, product]),
  );
  const countLabel =
    items.length === 1 ? "1 producto" : `${items.length} productos`;

  return (
    <Container as="main" className="py-8 md:py-16">
      <FavoritesHeading />
      <p className="mt-2 text-sm text-sf-muted">{countLabel}</p>
      <ul className="mt-6 grid gap-3 md:mt-8 md:gap-4">
        {items.map((item) => (
          <FavoriteItemCard
            key={item.productId}
            item={item}
            catalogProduct={productsById.get(item.productId)}
          />
        ))}
      </ul>
    </Container>
  );
}

function FavoritesHeading() {
  return (
    <>
      <Link
        href="/catalog"
        className="mb-3 inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:text-sf-primary md:mb-4"
      >
        ← Catálogo
      </Link>
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Favoritos
      </h1>
    </>
  );
}

function FavoritesSkeleton() {
  return (
    <ul className="mt-6 grid gap-3 md:mt-8 md:gap-4" aria-hidden="true">
      {Array.from({ length: 2 }, (_, index) => (
        <li
          key={index}
          className="grid grid-cols-[3.75rem_minmax(0,1fr)] gap-3 rounded-2xl border border-sf-border bg-sf-surface p-3 md:grid-cols-[5rem_minmax(0,1fr)] md:gap-4 md:p-4"
        >
          <Skeleton className="aspect-square w-full rounded-xl" />
          <div className="min-w-0">
            <Skeleton className="h-5 w-2/3" />
            <Skeleton className="mt-1.5 h-3 w-1/3" />
            <Skeleton className="mt-2 h-5 w-24" />
            <Skeleton className="mt-3 h-11 w-40" />
          </div>
        </li>
      ))}
    </ul>
  );
}
