"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useProductsQuery } from "@/features/catalog/hooks";
import {
  shoppingListItemCount,
  validateShoppingListName,
} from "@/features/lists/api";
import { ListItemCard } from "@/features/lists/components/list-item-card";
import {
  useClearShoppingListMutation,
  useRenameShoppingListMutation,
  useShoppingListQuery,
} from "@/features/lists/hooks";
import {
  formatListInstant,
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
import { TextField } from "@/shared/ui/text-field";

export function ListDetailPageContent({
  shoppingListId,
}: {
  shoppingListId: string;
}) {
  const listQuery = useShoppingListQuery(shoppingListId);
  const productsQuery = useProductsQuery();
  const clearMutation = useClearShoppingListMutation(shoppingListId);
  const [renaming, setRenaming] = useState(false);

  if (listQuery.isPending) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <DetailSkeleton />
      </Container>
    );
  }

  if (listQuery.isError) {
    const notFound =
      isApiError(listQuery.error) &&
      listQuery.error.problem.code === "SHOPPING_LIST_NOT_FOUND";
    const message = isApiError(listQuery.error)
      ? messageForApiProblem(listQuery.error.problem)
      : "No se pudo cargar la lista.";
    return (
      <Container as="main" className="py-10 md:py-16">
        {notFound ? (
          <EmptyState
            title="No encontramos esta lista"
            description="Puede que la hayas borrado o que no tengas acceso."
            action={
              <Link href="/lists" className={buttonClassName("secondary")}>
                Volver a Mis listas
              </Link>
            }
          />
        ) : (
          <div className="grid gap-4">
            <Alert tone="error" title="No se pudo cargar la lista">
              {message}
            </Alert>
            <Button type="button" variant="secondary" onClick={() => listQuery.refetch()}>
              Reintentar
            </Button>
          </div>
        )}
      </Container>
    );
  }

  const list = listQuery.data;
  if (!list) {
    return null;
  }

  const productsById = new Map(
    (productsQuery.data ?? []).map((product) => [product.id, product]),
  );
  const itemCount = shoppingListItemCount(list);
  const mutating = listQuery.isFetching && !listQuery.isPending;
  const clearError =
    clearMutation.isError && isApiError(clearMutation.error)
      ? messageForApiProblem(clearMutation.error.problem)
      : clearMutation.isError
        ? "No se pudo limpiar la lista."
        : null;

  return (
    <Container as="main" className="py-10 md:py-16">
      <Link
        href="/lists"
        className="text-sm font-semibold text-sf-primary hover:underline"
      >
        ← Mis listas
      </Link>

      <div className="mt-4 flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            {list.name}
          </h1>
          <p className="mt-2 text-sm text-sf-muted">
            {shoppingListItemsLabel(itemCount)} · Actualizada{" "}
            {formatListInstant(list.updatedAt)}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="secondary"
            onClick={() => setRenaming((open) => !open)}
          >
            {renaming ? "Cerrar" : "Renombrar"}
          </Button>
          {itemCount > 0 ? (
            <Button
              type="button"
              variant="ghost"
              disabled={clearMutation.isPending || mutating}
              onClick={() => void clearMutation.mutateAsync()}
            >
              {clearMutation.isPending ? "Limpiando…" : "Limpiar lista"}
            </Button>
          ) : null}
        </div>
      </div>

      {renaming ? (
        <Card className="mt-6 p-4">
          <RenameListForm
            shoppingListId={list.id}
            initialName={list.name}
            onDone={() => setRenaming(false)}
          />
        </Card>
      ) : null}

      {clearError ? (
        <div className="mt-6">
          <Alert tone="error" title="No se pudo limpiar">
            {clearError}
          </Alert>
        </div>
      ) : null}

      {itemCount === 0 ? (
        <div className="mt-8">
          <EmptyState
            title="Esta lista está vacía"
            description="Agrega productos desde el catálogo para armar tu mercado."
            action={
              <Link href="/catalog" className={buttonClassName("primary")}>
                Ir al catálogo
              </Link>
            }
          />
        </div>
      ) : (
        <ul className="mt-8 grid gap-4">
          {list.items.map((item) => (
            <li key={item.id}>
              <ListItemCard
                shoppingListId={list.id}
                item={item}
                product={productsById.get(item.productId)}
                busy={mutating}
              />
            </li>
          ))}
        </ul>
      )}
    </Container>
  );
}

function RenameListForm({
  shoppingListId,
  initialName,
  onDone,
}: {
  shoppingListId: string;
  initialName: string;
  onDone: () => void;
}) {
  const renameMutation = useRenameShoppingListMutation(shoppingListId);
  const [name, setName] = useState(initialName);
  const [fieldError, setFieldError] = useState<string | undefined>();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const error = validateShoppingListName(name);
    setFieldError(error);
    if (error) {
      return;
    }
    const trimmed = name.trim();
    if (trimmed === initialName.trim()) {
      onDone();
      return;
    }
    try {
      await renameMutation.mutateAsync({ name: trimmed });
      onDone();
    } catch {
      // surfaced below
    }
  }

  const errorMessage =
    renameMutation.isError && isApiError(renameMutation.error)
      ? messageForApiProblem(renameMutation.error.problem)
      : renameMutation.isError
        ? "No se pudo renombrar la lista."
        : null;

  return (
    <form className="grid gap-3" onSubmit={(event) => void handleSubmit(event)} noValidate>
      <TextField
        id={`rename-list-${shoppingListId}`}
        label="Nuevo nombre"
        value={name}
        error={fieldError}
        maxLength={255}
        autoFocus
        onChange={(event) => {
          setName(event.target.value);
          setFieldError(undefined);
        }}
      />
      {errorMessage ? (
        <Alert tone="error" title="No se pudo renombrar">
          {errorMessage}
        </Alert>
      ) : null}
      <div className="flex flex-wrap gap-2">
        <Button type="submit" disabled={renameMutation.isPending}>
          {renameMutation.isPending ? "Guardando…" : "Guardar"}
        </Button>
        <Button
          type="button"
          variant="secondary"
          disabled={renameMutation.isPending}
          onClick={onDone}
        >
          Cancelar
        </Button>
      </div>
    </form>
  );
}

function DetailSkeleton() {
  return (
    <div aria-hidden="true">
      <Skeleton className="h-4 w-24" />
      <Skeleton className="mt-4 h-10 w-2/3" />
      <Skeleton className="mt-3 h-4 w-1/3" />
      <div className="mt-8 grid gap-4">
        <Card className="grid gap-4 p-4 md:grid-cols-[8rem_1fr]">
          <Skeleton className="aspect-square w-full" />
          <div>
            <Skeleton className="h-5 w-2/3" />
            <Skeleton className="mt-3 h-4 w-1/3" />
            <Skeleton className="mt-6 h-11 w-40" />
          </div>
        </Card>
      </div>
    </div>
  );
}
