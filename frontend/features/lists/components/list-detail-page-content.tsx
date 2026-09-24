"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useProductsQuery } from "@/features/catalog/hooks";
import {
  shoppingListItemCount,
  validateShoppingListName,
} from "@/features/lists/api";
import { ListItemCard } from "@/features/lists/components/list-item-card";
import {
  useClearShoppingListMutation,
  useDeleteShoppingListMutation,
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
  const router = useRouter();
  const listQuery = useShoppingListQuery(shoppingListId);
  const productsQuery = useProductsQuery();
  const clearMutation = useClearShoppingListMutation(shoppingListId);
  const deleteMutation = useDeleteShoppingListMutation(shoppingListId);
  const [renaming, setRenaming] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  if (listQuery.isPending) {
    return (
      <Container as="main" className="py-8 md:py-16">
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
      <Container as="main" className="py-8 md:py-16">
        <Link
          href="/lists"
          className="mb-3 inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:text-sf-primary md:mb-4"
        >
          ← Mis listas
        </Link>
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
  const deleteError =
    deleteMutation.isError && isApiError(deleteMutation.error)
      ? messageForApiProblem(deleteMutation.error.problem)
      : deleteMutation.isError
        ? "No se pudo eliminar la lista."
        : null;

  async function handleDelete() {
    try {
      await deleteMutation.mutateAsync();
      router.push("/lists");
    } catch {
      // surfaced below
    }
  }

  return (
    <Container as="main" className="py-8 md:py-16">
      <Link
        href="/lists"
        className="mb-3 inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:text-sf-primary md:mb-4"
      >
        ← Mis listas
      </Link>

      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        {list.name}
      </h1>
      <p className="mt-2 text-sm text-sf-muted">
        {shoppingListItemsLabel(itemCount)}
      </p>
      <p className="mt-1 text-xs text-sf-muted">
        Actualizada {formatListInstant(list.updatedAt)}
      </p>

      <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-1">
        <Button
          type="button"
          variant="ghost"
          className="min-h-11 justify-start px-0 text-sm font-semibold text-sf-muted hover:bg-transparent hover:text-sf-primary"
          onClick={() => setRenaming((open) => !open)}
        >
          {renaming ? "Cerrar" : "Renombrar"}
        </Button>
        {itemCount > 0 ? (
          <Button
            type="button"
            variant="ghost"
            className="min-h-11 justify-start px-0 text-sm font-semibold text-sf-muted hover:bg-transparent hover:text-sf-error"
            disabled={clearMutation.isPending || mutating || deleteMutation.isPending}
            onClick={() => void clearMutation.mutateAsync()}
          >
            {clearMutation.isPending ? "Limpiando…" : "Limpiar lista"}
          </Button>
        ) : null}
      </div>

      {renaming ? (
        <Card className="mt-4 grid gap-3 !p-4 md:mt-5">
          <RenameListForm
            shoppingListId={list.id}
            initialName={list.name}
            onDone={() => setRenaming(false)}
          />
        </Card>
      ) : null}

      {clearError ? (
        <div className="mt-4 md:mt-5">
          <Alert tone="error" title="No se pudo limpiar">
            {clearError}
          </Alert>
        </div>
      ) : null}

      {deleteError ? (
        <div className="mt-4 md:mt-5">
          <Alert tone="error" title="No se pudo eliminar">
            {deleteError}
          </Alert>
        </div>
      ) : null}

      <div className="mt-4 md:mt-5">
        {confirmingDelete ? (
          <div className="grid gap-2 rounded-xl border border-sf-border bg-sf-surface p-4">
            <p className="text-sm font-semibold text-sf-ink">
              ¿Eliminar esta lista?
            </p>
            <p className="text-sm text-sf-muted">
              Se borrarán la lista y sus productos guardados. Los productos del
              catálogo no se eliminan.
            </p>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Button
                type="button"
                variant="destructive"
                className="flex-1"
                disabled={deleteMutation.isPending}
                onClick={() => void handleDelete()}
              >
                {deleteMutation.isPending ? "Eliminando…" : "Sí, eliminar"}
              </Button>
              <Button
                type="button"
                variant="secondary"
                className="flex-1"
                disabled={deleteMutation.isPending}
                onClick={() => setConfirmingDelete(false)}
              >
                Conservar lista
              </Button>
            </div>
          </div>
        ) : (
          <Button
            type="button"
            variant="ghost"
            className="min-h-11 justify-start px-0 text-sm font-semibold text-sf-muted hover:bg-transparent hover:text-sf-error"
            disabled={mutating || clearMutation.isPending}
            onClick={() => setConfirmingDelete(true)}
          >
            Eliminar lista
          </Button>
        )}
      </div>

      {itemCount === 0 ? (
        <div className="mt-6 md:mt-8">
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
        <ul className="mt-6 grid gap-3 md:mt-8 md:gap-4">
          {list.items.map((item) => (
            <ListItemCard
              key={item.id}
              shoppingListId={list.id}
              item={item}
              product={productsById.get(item.productId)}
              busy={mutating}
            />
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
      <Skeleton className="mb-3 h-5 w-28" />
      <Skeleton className="h-9 w-2/3 md:h-11" />
      <Skeleton className="mt-2 h-4 w-28" />
      <Skeleton className="mt-1 h-3 w-40" />
      <ul className="mt-6 grid gap-3 md:mt-8 md:gap-4">
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
    </div>
  );
}
