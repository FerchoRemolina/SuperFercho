"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useProductsQuery } from "@/features/catalog/hooks";
import {
  shoppingListItemCount,
  shoppingListPurchaseSummary,
  validateShoppingListName,
} from "@/features/lists/api";
import { ListItemCard } from "@/features/lists/components/list-item-card";
import {
  useAddShoppingListToCartMutation,
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
import { ClipboardListIcon } from "@/shared/ui/icons";
import { Skeleton } from "@/shared/ui/skeleton";
import { TextField } from "@/shared/ui/text-field";
import { cx } from "@/shared/utils/cx";

const BUY_UNAVAILABLE_MESSAGE =
  "No pudimos agregar esta lista al carrito porque uno de sus productos ya no está disponible.";
const BUY_GENERIC_MESSAGE =
  "No pudimos agregar esta lista al carrito. Inténtalo de nuevo en unos minutos.";

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
  const buyMutation = useAddShoppingListToCartMutation();
  const [renaming, setRenaming] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  if (listQuery.isPending) {
    return (
      <Container as="main" width="wide" className="py-8 md:py-16">
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
      <Container as="main" width="wide" className="py-8 md:py-16">
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
  const purchase = shoppingListPurchaseSummary(list);
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
  const buyError = buyMutation.isError
    ? isApiError(buyMutation.error) &&
        buyMutation.error.problem.code === "PRODUCT_NOT_FOUND"
      ? BUY_UNAVAILABLE_MESSAGE
      : BUY_GENERIC_MESSAGE
    : null;

  // Compra de la lista: una sola llamada al endpoint oficial del backend,
  // que agrega únicamente los productos con cantidad mayor a 0.
  async function buyList() {
    if (buyMutation.isPending) {
      return;
    }
    try {
      await buyMutation.mutateAsync(list.id);
      router.push("/cart");
    } catch {
      // surfaced below
    }
  }

  async function handleDelete() {
    try {
      await deleteMutation.mutateAsync();
      router.push("/lists");
    } catch {
      // surfaced below
    }
  }

  return (
    <Container as="main" width="wide" className="py-8 md:py-12">
      <Link
        href="/lists"
        className={cx(
          "mb-4 inline-flex min-h-9 items-center gap-1.5 rounded-full border border-sf-border bg-sf-surface px-3.5",
          "text-sm font-semibold text-sf-muted transition-colors duration-150",
          "hover:border-sf-primary/40 hover:text-sf-primary md:mb-6",
        )}
      >
        ← Mis listas
      </Link>

      <h1 className="break-words text-[1.75rem] font-bold leading-tight tracking-tight text-sf-ink md:text-[2.25rem]">
        {list.name}
      </h1>
      <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1">
        <p className="text-sm text-sf-muted">
          {shoppingListItemsLabel(itemCount)}
        </p>
        <span aria-hidden="true" className="text-sf-border">·</span>
        <p className="text-xs text-sf-muted">
          Actualizada {formatListInstant(list.updatedAt)}
        </p>
      </div>

      <div className="mt-5 flex flex-wrap items-center gap-2 border-y border-sf-border py-3">
        <Button
          type="button"
          variant="ghost"
          className="min-h-10 px-3 text-sm font-semibold text-sf-primary hover:bg-sf-primary-soft"
          onClick={() => setRenaming((open) => !open)}
        >
          {renaming ? "Cerrar" : "Renombrar"}
        </Button>
        {itemCount > 0 ? (
          <Button
            type="button"
            variant="ghost"
            className="min-h-10 px-3 text-sm font-semibold text-sf-muted hover:bg-transparent hover:text-sf-error"
            disabled={clearMutation.isPending || mutating || deleteMutation.isPending}
            onClick={() => void clearMutation.mutateAsync()}
          >
            {clearMutation.isPending ? "Limpiando…" : "Limpiar lista"}
          </Button>
        ) : null}
        {!confirmingDelete ? (
          <Button
            type="button"
            variant="ghost"
            className="min-h-10 px-3 text-sm font-semibold text-sf-muted hover:bg-transparent hover:text-sf-error"
            disabled={mutating || clearMutation.isPending}
            onClick={() => setConfirmingDelete(true)}
          >
            Eliminar lista
          </Button>
        ) : null}
      </div>

      <Card className="mt-4 flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between md:gap-6 md:p-5">
        <div className="min-w-0">
          <p className="text-base font-semibold text-sf-ink md:text-lg">
            Comprar esta lista
          </p>
          {purchase.itemCount > 0 ? (
            <p className="mt-0.5 text-sm text-sf-muted">
              {shoppingListItemsLabel(purchase.itemCount)} por comprar ·{" "}
              {purchase.unitCount}{" "}
              {purchase.unitCount === 1 ? "unidad" : "unidades"} en total
            </p>
          ) : (
            <p className="mt-0.5 text-sm text-sf-muted">
              Todos los productos están guardados con cantidad 0. Aumenta
              la cantidad de al menos uno para incluirlo en la compra.
            </p>
          )}
        </div>
        <Button
          type="button"
          className="w-full shrink-0 md:w-auto"
          disabled={buyMutation.isPending || purchase.itemCount === 0}
          onClick={() => void buyList()}
        >
          {buyMutation.isPending ? "Agregando al carrito…" : "Comprar esta lista"}
        </Button>
      </Card>

      {renaming ? (
        <Card className="mt-4 grid gap-3 !p-4 md:mt-5 md:!p-5">
          <RenameListForm
            shoppingListId={list.id}
            initialName={list.name}
            onDone={() => setRenaming(false)}
          />
        </Card>
      ) : null}

      {buyError ? (
        <div className="mt-4 md:mt-5" role="alert">
          <Alert tone="error" title="No se pudo comprar la lista">
            {buyError}
          </Alert>
        </div>
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

      {confirmingDelete ? (
        <div className="mt-4 grid gap-2 rounded-xl border border-sf-error/30 bg-red-50/60 p-4 md:mt-5">
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
      ) : null}

      {itemCount === 0 ? (
        <div className="mt-6 md:mt-8">
          <EmptyState
            title="Esta lista está vacía"
            description="Agrega productos desde el catálogo para armar tu mercado."
            align="center"
            className="py-14"
            icon={<ClipboardListIcon className="size-6" />}
            action={
              <Link href="/catalog" className={buttonClassName("primary")}>
                Ir al catálogo
              </Link>
            }
          />
        </div>
      ) : (
        <>
          <div className="mt-8 flex items-baseline justify-between gap-3 md:mt-10">
            <h2 className="text-sm font-semibold uppercase tracking-[0.08em] text-sf-muted">
              Productos
            </h2>
            <p className="text-xs text-sf-muted">
              {shoppingListItemsLabel(itemCount)} guardados
            </p>
          </div>
          <ul className="mt-3 grid grid-cols-1 gap-3 md:mt-4 md:grid-cols-2 md:gap-4">
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
        </>
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
      <Skeleton className="mb-4 h-9 w-32 rounded-full" />
      <Skeleton className="h-9 w-2/3 md:h-11" />
      <Skeleton className="mt-2 h-4 w-28" />
      <div className="mt-5 border-y border-sf-border py-3">
        <Skeleton className="h-6 w-48" />
      </div>
      <Skeleton className="mt-4 h-20 w-full rounded-xl md:h-24" />
      <div className="mt-8 flex items-center justify-between">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-4 w-20" />
      </div>
      <ul className="mt-3 grid grid-cols-1 gap-3 md:mt-4 md:grid-cols-2 md:gap-4">
        {Array.from({ length: 4 }, (_, index) => (
          <li
            key={index}
            className="grid grid-cols-[3.5rem_minmax(0,1fr)] items-start gap-3 rounded-xl border border-sf-border bg-sf-surface p-3 shadow-[0_1px_2px_rgba(23,33,27,0.05)]"
          >
            <Skeleton className="aspect-square w-full rounded-lg" />
            <div className="min-w-0">
              <Skeleton className="h-4 w-1/2" />
              <Skeleton className="mt-1.5 h-3 w-24" />
              <Skeleton className="mt-1.5 h-4 w-32" />
            </div>
            <div className="col-span-2 flex items-center justify-between border-t border-sf-border/70 pt-2.5">
              <Skeleton className="h-11 w-32 rounded-lg" />
              <Skeleton className="h-8 w-20 rounded-lg" />
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
