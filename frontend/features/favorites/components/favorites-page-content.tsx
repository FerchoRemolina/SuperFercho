"use client";

import Link from "next/link";
import { FavoriteItemCard } from "@/features/favorites/components/favorite-item-card";
import { useFavoritesQuery } from "@/features/favorites/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { HeartIcon } from "@/shared/ui/icons";
import { PageHeader } from "@/shared/ui/page-header";
import { Skeleton } from "@/shared/ui/skeleton";

export function FavoritesPageContent() {
  const favoritesQuery = useFavoritesQuery();

  if (favoritesQuery.isPending) {
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
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
      <Container as="main" width="wide" className="py-8 md:py-12">
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
      <Container as="main" width="wide" className="py-8 md:py-12">
        <FavoritesHeading />
        <div className="mt-6 md:mt-8">
          <EmptyState
            title="Aún no tienes favoritos"
            description="Guarda los productos que más te gustan para encontrarlos fácilmente después."
            align="center"
            className="py-14"
            icon={<HeartIcon className="size-6" />}
            action={
              <Link href="/catalog" className={buttonClassName("primary")}>
                Explorar catálogo
              </Link>
            }
          />
        </div>
      </Container>
    );
  }

  const countLabel =
    items.length === 1 ? "1 producto" : `${items.length} productos`;

  return (
    <Container as="main" width="wide" className="py-8 md:py-12">
      <FavoritesHeading />
      <p className="mt-2.5 inline-flex items-center rounded-full bg-sf-primary-soft px-3 py-1 text-xs font-semibold text-sf-primary">
        {countLabel}
      </p>
      <ul className="mt-6 grid grid-cols-1 gap-3 md:mt-8 md:grid-cols-3 md:gap-4 xl:grid-cols-4 xl:gap-5">
        {items.map((item) => (
          <FavoriteItemCard key={item.productId} item={item} />
        ))}
      </ul>
    </Container>
  );
}

function FavoritesHeading() {
  return (
    <PageHeader
      title="Favoritos"
      back={{ href: "/catalog", label: "← Catálogo" }}
    />
  );
}

function FavoritesSkeleton() {
  return (
    <ul className="mt-6 grid grid-cols-1 gap-3 md:mt-8 md:grid-cols-3 md:gap-4 xl:grid-cols-4 xl:gap-5" aria-hidden="true">
      {Array.from({ length: 6 }, (_, index) => (
        <li
          key={index}
          className="overflow-hidden rounded-2xl border border-sf-border bg-sf-surface"
        >
          <div className="flex items-start gap-3 p-3">
            <Skeleton className="size-20 rounded-lg" />
            <div className="grid flex-1 gap-2">
              <Skeleton className="h-3 w-1/3" />
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-4 w-1/2" />
            </div>
          </div>
          <div className="p-3 pt-0">
            <Skeleton className="h-9 w-full" />
          </div>
        </li>
      ))}
    </ul>
  );
}
