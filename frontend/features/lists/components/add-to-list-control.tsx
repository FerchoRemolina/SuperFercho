"use client";

import { useId, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  useAddShoppingListItemMutation,
  useCreateShoppingListMutation,
  useShoppingListsQuery,
} from "@/features/lists/hooks";
import { shoppingListHref } from "@/features/lists/presentation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { useSession } from "@/shared/session/session-provider";
import { Button, buttonClassName } from "@/shared/ui/button";
import { cx } from "@/shared/utils/cx";

export function AddToListControl({
  productId,
  className,
}: {
  productId: string;
  className?: string;
}) {
  const { session } = useSession();
  const router = useRouter();
  const listsQuery = useShoppingListsQuery();
  const addMutation = useAddShoppingListItemMutation();
  const createMutation = useCreateShoppingListMutation();
  const [open, setOpen] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const menuId = useId();

  if (session?.role === "ADMIN") {
    return null;
  }

  if (!session) {
    return (
      <Link
        href={`/login?next=${encodeURIComponent(`/products/${productId}`)}`}
        className={buttonClassName("secondary", className)}
      >
        Añadir a lista
      </Link>
    );
  }

  const lists = listsQuery.data ?? [];
  const busy = addMutation.isPending || createMutation.isPending;

  const errorMessage =
    (addMutation.isError && isApiError(addMutation.error)
      ? messageForApiProblem(addMutation.error.problem)
      : addMutation.isError
        ? "No se pudo agregar a la lista."
        : null) ??
    (createMutation.isError && isApiError(createMutation.error)
      ? messageForApiProblem(createMutation.error.problem)
      : createMutation.isError
        ? "No se pudo crear la lista."
        : null);

  async function addToList(shoppingListId: string) {
    setFeedback(null);
    try {
      await addMutation.mutateAsync({
        shoppingListId,
        body: { productId, quantity: 1 },
      });
      setFeedback("Agregado a la lista.");
      setOpen(false);
      window.setTimeout(() => setFeedback(null), 2000);
    } catch {
      // surfaced below
    }
  }

  async function createAndAdd() {
    setFeedback(null);
    try {
      const list = await createMutation.mutateAsync({
        name: "Mi lista",
      });
      await addMutation.mutateAsync({
        shoppingListId: list.id,
        body: { productId, quantity: 1 },
      });
      setOpen(false);
      router.push(shoppingListHref(list.id));
    } catch {
      // surfaced below
    }
  }

  return (
    <div className={cx("relative grid gap-2", className)}>
      <Button
        type="button"
        variant="secondary"
        className="w-full"
        aria-expanded={open}
        aria-controls={menuId}
        disabled={busy || listsQuery.isPending}
        onClick={() => setOpen((value) => !value)}
      >
        {busy ? "Guardando…" : "Añadir a lista"}
      </Button>
      {feedback ? (
        <p className="text-sm font-semibold text-sf-success" aria-live="polite">
          {feedback}
        </p>
      ) : null}
      {errorMessage ? (
        <p className="text-sm text-sf-error">{errorMessage}</p>
      ) : null}
      {open ? (
        <div
          id={menuId}
          className="absolute left-0 right-0 top-full z-20 mt-2 rounded-xl border border-sf-border bg-sf-surface p-2 shadow-[0_8px_24px_rgba(23,33,27,0.08)]"
        >
          {listsQuery.isError ? (
            <p className="px-3 py-2 text-sm text-sf-error">
              No se pudieron cargar tus listas.
            </p>
          ) : null}
          {lists.length === 0 ? (
            <button
              type="button"
              className="flex min-h-11 w-full items-center rounded-lg px-3 text-left text-sm font-semibold text-sf-ink hover:bg-sf-bg"
              disabled={busy}
              onClick={() => void createAndAdd()}
            >
              Crear lista y agregar
            </button>
          ) : (
            <ul className="grid gap-1">
              {lists.map((list) => (
                <li key={list.id}>
                  <button
                    type="button"
                    className="flex min-h-11 w-full items-center rounded-lg px-3 text-left text-sm font-semibold text-sf-ink hover:bg-sf-bg"
                    disabled={busy}
                    onClick={() => void addToList(list.id)}
                  >
                    {list.name}
                  </button>
                </li>
              ))}
              <li>
                <Link
                  href="/lists"
                  className="flex min-h-11 w-full items-center rounded-lg px-3 text-sm font-semibold text-sf-primary hover:bg-sf-bg"
                >
                  Gestionar listas
                </Link>
              </li>
            </ul>
          )}
        </div>
      ) : null}
    </div>
  );
}
