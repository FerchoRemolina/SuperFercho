"use client";

import Link from "next/link";
import { useProductsQuery } from "@/features/catalog/hooks";
import { FavoriteItemCard } from "@/features/favorites/components/favorite-item-card";
import { useFavoritesQuery } from "@/features/favorites/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function FavoritesPageContent() {
  const favoritesQuery = useFavoritesQuery();
  const productsQuery = useProductsQuery();

  if (favoritesQuery.isPending) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Favoritos
        </h1>
        <FavoritesSkeleton />
      </Container>
    );
  }

  if (favoritesQuery.isError) {
    const message = isApiError(favoritesQuery.error)
      ? messageForApiProblem(favoritesQuery.error.problem)
      : "No se pudieron cargar tus favoritos.";
    return (
      <Container as="main" className="py-10 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Favoritos
        </h1>
        <div className="mt-8 grid gap-4">
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
      <Container as="main" className="py-10 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Favoritos
        </h1>
        <div className="mt-8">
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

  return (
    <Container as="main" className="py-10 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Favoritos
      </h1>
      <ul className="mt-8 grid gap-4">
        {items.map((item) => (
          <li key={item.productId}>
            <FavoriteItemCard
              item={item}
              catalogProduct={productsById.get(item.productId)}
            />
          </li>
        ))}
      </ul>
    </Container>
  );
}

function FavoritesSkeleton() {
  return (
    <div className="mt-8 grid gap-4" aria-hidden="true">
      <Card className="grid gap-4 p-4 md:grid-cols-[8rem_1fr]">
        <Skeleton className="aspect-square w-full" />
        <div>
          <Skeleton className="h-5 w-2/3" />
          <Skeleton className="mt-3 h-4 w-1/3" />
          <Skeleton className="mt-6 h-11 w-40" />
        </div>
      </Card>
      <Card className="grid gap-4 p-4 md:grid-cols-[8rem_1fr]">
        <Skeleton className="aspect-square w-full" />
        <div>
          <Skeleton className="h-5 w-1/2" />
          <Skeleton className="mt-3 h-4 w-1/4" />
          <Skeleton className="mt-6 h-11 w-40" />
        </div>
      </Card>
    </div>
  );
}
