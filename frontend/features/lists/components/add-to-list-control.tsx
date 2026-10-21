"use client";

import { useEffect, useId, useRef, useState } from "react";
import Link from "next/link";
import {
  shoppingListContainsProduct,
  type ShoppingList,
} from "@/features/lists/api";
import {
  useAddShoppingListItemMutation,
  useShoppingListsQuery,
} from "@/features/lists/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { useSession } from "@/shared/session/session-provider";
import { Button, buttonClassName } from "@/shared/ui/button";
import { CheckCircleIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

const HOVER_CLOSE_DELAY_MS = 180;
const ALREADY_IN_LIST_MESSAGE = "El producto ya está en esta lista.";

export function AddToListControl({
  productId,
  className,
  onOpenChange,
}: {
  productId: string;
  className?: string;
  /** Notifica si el menú de listas está abierto (p. ej. para elevar la card en el grid). */
  onOpenChange?: (open: boolean) => void;
}) {
  const { session } = useSession();
  const listsQuery = useShoppingListsQuery();
  const addMutation = useAddShoppingListItemMutation();
  const [open, setOpen] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const menuId = useId();
  const zoneRef = useRef<HTMLDivElement>(null);
  const closeTimerRef = useRef<number | null>(null);

  function setOpenAndNotify(next: boolean) {
    setOpen(next);
    onOpenChange?.(next);
  }

  function cancelScheduledClose() {
    if (closeTimerRef.current !== null) {
      window.clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
  }

  useEffect(() => {
    return () => {
      if (closeTimerRef.current !== null) {
        window.clearTimeout(closeTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (!open) {
      return;
    }
    function closeMenu() {
      setOpen(false);
      onOpenChange?.(false);
    }
    function onPointerDown(event: PointerEvent) {
      if (!zoneRef.current?.contains(event.target as Node)) {
        closeMenu();
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        closeMenu();
      }
    }
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open, onOpenChange]);

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
  if (!listsQuery.isPending && lists.length === 0) {
    return null;
  }

  const busy = addMutation.isPending;

  const errorMessage =
    addMutation.isError && isApiError(addMutation.error)
      ? messageForApiProblem(addMutation.error.problem)
      : addMutation.isError
        ? "No se pudo agregar a la lista."
        : null;

  async function addToList(list: ShoppingList) {
    setFeedback(null);
    if (shoppingListContainsProduct(list, productId)) {
      // Pertenencia existente: idempotente, sin request ni cambios de cantidad.
      setFeedback(ALREADY_IN_LIST_MESSAGE);
      return;
    }
    try {
      const result = await addMutation.mutateAsync({
        shoppingListId: list.id,
        body: { productId, quantity: 1 },
      });
      if (result.alreadyPresent) {
        // El menú tenía datos desactualizados; el backend confirma pertenencia.
        setFeedback(ALREADY_IN_LIST_MESSAGE);
        return;
      }
      setFeedback("Agregado a la lista.");
      setOpenAndNotify(false);
      window.setTimeout(() => setFeedback(null), 2000);
    } catch {
      // surfaced below
    }
  }

  return (
    <div
      ref={zoneRef}
      className={cx("relative grid gap-2", className)}
      onMouseEnter={cancelScheduledClose}
      onMouseLeave={() => {
        if (open) {
          cancelScheduledClose();
          closeTimerRef.current = window.setTimeout(() => {
            closeTimerRef.current = null;
            setOpenAndNotify(false);
          }, HOVER_CLOSE_DELAY_MS);
        }
      }}
    >
      <Button
        type="button"
        variant="secondary"
        className="w-full"
        aria-expanded={open}
        aria-controls={menuId}
        disabled={busy || listsQuery.isPending}
        onClick={() => {
          if (!open) {
            setFeedback(null);
          }
          setOpenAndNotify(!open);
        }}
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
          role="menu"
          aria-label="Añadir a una lista"
          className="absolute inset-x-0 top-full z-40 mt-1.5 max-h-64 overflow-y-auto rounded-xl border border-sf-border bg-sf-surface p-2 shadow-[0_12px_32px_rgba(23,33,27,0.14)]"
        >
          {listsQuery.isError ? (
            <p className="px-3 py-2 text-sm text-sf-error">
              No se pudieron cargar tus listas.
            </p>
          ) : null}
          <ul className="grid gap-1">
            {lists.map((list) => {
              const contains = shoppingListContainsProduct(list, productId);
              return (
                <li key={list.id}>
                  <button
                    type="button"
                    role="menuitem"
                    className="flex min-h-11 w-full items-center gap-2 rounded-lg px-3 text-left text-sm font-semibold text-sf-ink transition-colors hover:bg-sf-bg"
                    disabled={busy}
                    onClick={() => void addToList(list)}
                  >
                    {contains ? (
                      <CheckCircleIcon
                        width={16}
                        height={16}
                        className="shrink-0 text-sf-success"
                      />
                    ) : null}
                    <span className="grid min-w-0 flex-1 gap-0.5">
                      <span className="truncate">{list.name}</span>
                      {contains ? (
                        <span className="text-xs font-medium text-sf-muted">
                          Ya está en esta lista
                        </span>
                      ) : null}
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}
    </div>
  );
}
