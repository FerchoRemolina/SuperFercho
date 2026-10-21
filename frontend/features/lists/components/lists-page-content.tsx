"use client";

import { useState } from "react";
import Link from "next/link";
import {
  shoppingListItemCount,
  type ShoppingList,
} from "@/features/lists/api";
import { CreateListForm } from "@/features/lists/components/create-list-form";
import { useShoppingListsQuery } from "@/features/lists/hooks";
import {
  formatListInstant,
  shoppingListHref,
  shoppingListItemsLabel,
} from "@/features/lists/presentation";
import { useProductsQuery } from "@/features/catalog/hooks";
import type { Product } from "@/features/catalog/api";
import { ProductImage } from "@/features/catalog/components/product-image";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { ClipboardListIcon } from "@/shared/ui/icons";
import { PageHeader } from "@/shared/ui/page-header";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

const PREVIEW_LIMIT = 4;

export function ListsPageContent() {
  const listsQuery = useShoppingListsQuery();
  const productsQuery = useProductsQuery();
  const [creating, setCreating] = useState(false);

  if (listsQuery.isPending) {
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <PageHeader title="Mis listas" />
        <ListsSkeleton />
      </Container>
    );
  }

  if (listsQuery.isError) {
    const message = isApiError(listsQuery.error)
      ? messageForApiProblem(listsQuery.error.problem)
      : "No se pudieron cargar tus listas.";
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <PageHeader title="Mis listas" />
        <div className="mt-6 grid gap-4 md:mt-8">
          <Alert tone="error" title="No se pudieron cargar las listas">
            {message}
          </Alert>
          <Button type="button" variant="secondary" onClick={() => listsQuery.refetch()}>
            Reintentar
          </Button>
        </div>
      </Container>
    );
  }

  const lists = listsQuery.data ?? [];
  const hasLists = lists.length > 0;
  const productsById = new Map(
    (productsQuery.data ?? []).map((product) => [product.id, product]),
  );

  return (
    <Container as="main" width="wide" className="py-8 md:py-12">
      <PageHeader
        title="Mis listas"
        actions={
          !creating && hasLists ? (
            <Button type="button" onClick={() => setCreating(true)}>
              Nueva lista
            </Button>
          ) : null
        }
      />
      {!hasLists ? (
        <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
          Guarda productos y arma tu mercado a tu ritmo.
        </p>
      ) : null}

      {creating ? (
        <Card className="mt-6 grid gap-3 !p-4 md:mt-8 md:!p-5">
          <h2 className="text-base font-semibold text-sf-ink md:text-lg">
            Nueva lista
          </h2>
          <CreateListForm
            autofocus
            onCancel={() => setCreating(false)}
          />
        </Card>
      ) : null}

      {!hasLists && !creating ? (
        <div className="mt-6 md:mt-8">
          <EmptyState
            title="Todavía no tienes listas"
            description="Crea listas para cumpleaños, asados, ocasiones especiales o tu mercado semanal, y guarda productos a tu ritmo."
            align="center"
            className="py-14"
            icon={<ClipboardListIcon className="size-6" />}
            action={
              <Button type="button" onClick={() => setCreating(true)}>
                Crear lista
              </Button>
            }
          />
        </div>
      ) : null}

      {hasLists ? (
        <ul className="mt-6 grid gap-3 md:mt-8 md:grid-cols-2 md:gap-4 xl:grid-cols-3">
          {lists.map((list) => (
            <li key={list.id}>
              <ListSummaryCard list={list} productsById={productsById} />
            </li>
          ))}
        </ul>
      ) : null}

      {hasLists && !creating ? (
        <p className="mt-5 text-sm text-sf-muted">
          También puedes{" "}
          <Link href="/catalog" className="font-semibold text-sf-primary hover:underline">
            agregar productos desde el catálogo
          </Link>
          .
        </p>
      ) : null}
    </Container>
  );
}

function ListSummaryCard({
  list,
  productsById,
}: {
  list: ShoppingList;
  productsById: Map<string, Product>;
}) {
  const itemCount = shoppingListItemCount(list);
  const previews = list.items
    .map((item) => productsById.get(item.productId))
    .filter((product) => product !== undefined)
    .slice(0, PREVIEW_LIMIT);
  const extraCount = Math.max(0, itemCount - previews.length);

  return (
    <Link href={shoppingListHref(list.id)} className="group block">
      <Card className="sf-lift grid gap-3 !p-4 hover:border-sf-primary/30 md:!p-5">
        <div className="flex items-start justify-between gap-3">
          <p className="min-w-0 truncate text-base font-semibold tracking-tight text-sf-ink md:text-lg">
            {list.name}
          </p>
          <span className="shrink-0 rounded-full bg-sf-primary-soft px-2.5 py-0.5 text-xs font-semibold text-sf-primary">
            {shoppingListItemsLabel(itemCount)}
          </span>
        </div>
        {previews.length > 0 ? (
          <div className="flex items-center gap-1.5" aria-hidden="true">
            {previews.map((product) => (
              <div
                key={product.id}
                className="size-12 shrink-0 overflow-hidden rounded-lg"
              >
                <ProductImage
                  src={product.imageUrl}
                  alt=""
                  className="h-full w-full"
                />
              </div>
            ))}
            {extraCount > 0 ? (
              <span className="flex size-12 shrink-0 items-center justify-center rounded-lg border border-dashed border-sf-border text-xs font-semibold text-sf-muted">
                +{extraCount}
              </span>
            ) : null}
          </div>
        ) : null}
        <div className="flex items-center justify-between border-t border-sf-border pt-2.5">
          <p className="text-xs text-sf-muted">
            Actualizada {formatListInstant(list.updatedAt)}
          </p>
          <p
            className={cx(
              "text-sm font-semibold text-sf-muted transition-colors",
              "group-hover:text-sf-primary",
            )}
          >
            Ver lista <span aria-hidden="true">→</span>
          </p>
        </div>
      </Card>
    </Link>
  );
}

function ListsSkeleton() {
  return (
    <div
      className="mt-6 grid gap-3 md:mt-8 md:grid-cols-2 md:gap-4 xl:grid-cols-3"
      aria-hidden="true"
    >
      {Array.from({ length: 3 }, (_, index) => (
        <Card key={index} className="grid gap-2.5 !p-4 md:!p-5">
          <div className="flex items-start gap-3">
            <Skeleton className="size-10 rounded-xl" />
            <div>
              <Skeleton className="h-5 w-32" />
              <Skeleton className="mt-1.5 h-4 w-20" />
            </div>
          </div>
          <Skeleton className="h-3 w-40" />
        </Card>
      ))}
    </div>
  );
}
