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
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function ListsPageContent() {
  const listsQuery = useShoppingListsQuery();
  const [creating, setCreating] = useState(false);

  if (listsQuery.isPending) {
    return (
      <Container as="main" className="py-8 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Mis listas
        </h1>
        <ListsSkeleton />
      </Container>
    );
  }

  if (listsQuery.isError) {
    const message = isApiError(listsQuery.error)
      ? messageForApiProblem(listsQuery.error.problem)
      : "No se pudieron cargar tus listas.";
    return (
      <Container as="main" className="py-8 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Mis listas
        </h1>
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

  return (
    <Container as="main" className="py-8 md:py-16">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="min-w-0">
          <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            Mis listas
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
            Guarda productos y arma tu mercado a tu ritmo.
          </p>
        </div>
        {!creating && hasLists ? (
          <Button type="button" onClick={() => setCreating(true)}>
            Nueva lista
          </Button>
        ) : null}
      </div>

      {creating ? (
        <Card className="mt-6 grid gap-3 !p-4 md:mt-8">
          <h2 className="text-lg font-semibold text-sf-ink">Nueva lista</h2>
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
            description="Crea una lista para guardar productos y armar tu mercado con calma."
            action={
              <Button type="button" onClick={() => setCreating(true)}>
                Crear lista
              </Button>
            }
          />
        </div>
      ) : null}

      {hasLists ? (
        <ul className="mt-6 grid gap-3 md:mt-8 md:gap-4">
          {lists.map((list) => (
            <li key={list.id}>
              <ListSummaryCard list={list} />
            </li>
          ))}
        </ul>
      ) : null}

      {hasLists && !creating ? (
        <p className="mt-5 text-sm text-sf-muted">
          También puedes{" "}
          <Link href="/catalog" className="font-semibold text-sf-primary">
            agregar productos desde el catálogo
          </Link>
          .
        </p>
      ) : null}
    </Container>
  );
}

function ListSummaryCard({ list }: { list: ShoppingList }) {
  const itemCount = shoppingListItemCount(list);
  return (
    <Link href={shoppingListHref(list.id)} className="block">
      <Card className="grid gap-1.5 !p-4 transition-shadow hover:shadow-[0_8px_24px_rgba(23,33,27,0.08)]">
        <p className="text-base font-semibold tracking-tight text-sf-ink md:text-lg">
          {list.name}
        </p>
        <p className="text-sm font-medium text-sf-ink">
          {shoppingListItemsLabel(itemCount)}
        </p>
        <p className="text-xs text-sf-muted">
          Actualizada {formatListInstant(list.updatedAt)}
        </p>
        <p className="pt-0.5 text-sm font-semibold text-sf-muted">Ver lista</p>
      </Card>
    </Link>
  );
}

function ListsSkeleton() {
  return (
    <div className="mt-6 grid gap-3 md:mt-8 md:gap-4" aria-hidden="true">
      {Array.from({ length: 2 }, (_, index) => (
        <Card key={index} className="grid gap-1.5 !p-4">
          <Skeleton className="h-5 w-1/2" />
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-3 w-36" />
        </Card>
      ))}
    </div>
  );
}
