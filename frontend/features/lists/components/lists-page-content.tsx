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
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function ListsPageContent() {
  const listsQuery = useShoppingListsQuery();
  const [creating, setCreating] = useState(false);

  if (listsQuery.isPending) {
    return (
      <Container as="main" className="py-10 md:py-16">
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
      <Container as="main" className="py-10 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Mis listas
        </h1>
        <div className="mt-8 grid gap-4">
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

  return (
    <Container as="main" className="py-10 md:py-16">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Mis listas
        </h1>
        {!creating ? (
          <Button type="button" onClick={() => setCreating(true)}>
            Nueva lista
          </Button>
        ) : null}
      </div>

      {creating ? (
        <Card className="mt-8 grid gap-3 p-4">
          <h2 className="text-lg font-semibold text-sf-ink">Nueva lista</h2>
          <CreateListForm
            autofocus
            onCancel={() => setCreating(false)}
          />
        </Card>
      ) : null}

      {lists.length === 0 && !creating ? (
        <div className="mt-8">
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

      {lists.length > 0 ? (
        <ul className="mt-8 grid gap-4">
          {lists.map((list) => (
            <li key={list.id}>
              <ListSummaryCard list={list} />
            </li>
          ))}
        </ul>
      ) : null}

      {lists.length > 0 && !creating ? (
        <p className="mt-6 text-sm text-sf-muted">
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
    <Card className="p-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <h2 className="text-lg font-semibold text-sf-ink">
            <Link
              href={shoppingListHref(list.id)}
              className="hover:text-sf-primary"
            >
              {list.name}
            </Link>
          </h2>
          <p className="mt-1 text-sm text-sf-muted">
            {shoppingListItemsLabel(itemCount)}
          </p>
          <p className="mt-1 text-sm text-sf-muted">
            Actualizada {formatListInstant(list.updatedAt)}
          </p>
        </div>
        <Link
          href={shoppingListHref(list.id)}
          className={buttonClassName("secondary")}
        >
          Abrir
        </Link>
      </div>
    </Card>
  );
}

function ListsSkeleton() {
  return (
    <div className="mt-8 grid gap-4" aria-hidden="true">
      <Card className="p-4">
        <Skeleton className="h-5 w-1/2" />
        <Skeleton className="mt-3 h-4 w-1/4" />
      </Card>
      <Card className="p-4">
        <Skeleton className="h-5 w-2/3" />
        <Skeleton className="mt-3 h-4 w-1/3" />
      </Card>
    </div>
  );
}
