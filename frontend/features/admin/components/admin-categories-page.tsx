"use client";

import { useEffect, useId, useMemo, useRef, useState } from "react";
import Link from "next/link";
import {
  useActivateAdminCategoryMutation,
  useAdminCategoriesQuery,
  useAdminProductsQuery,
  useDeactivateAdminCategoryMutation,
} from "@/features/admin/hooks";
import { CategoryIcon } from "@/features/admin/components/category-icon";
import type { Category } from "@/features/catalog/api";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { EmptyState } from "@/shared/ui/empty-state";
import { InfoIcon, SearchIcon, TagIcon } from "@/shared/ui/icons";
import { SelectField } from "@/shared/ui/select-field";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

type StatusFilter = "" | "ACTIVE" | "INACTIVE";
type SortDir = "asc" | "desc";
type ConfirmAction = "activate" | "deactivate";

const CONFIRM_COPY: Record<
  ConfirmAction,
  { title: string; body: string; confirm: string; menuLabel: string }
> = {
  deactivate: {
    title: "¿Desactivar esta categoría?",
    body: "Dejará de verse en el catálogo público y sus productos no se podrán vender.",
    confirm: "Sí, desactivar",
    menuLabel: "Desactivar",
  },
  activate: {
    title: "¿Activar esta categoría?",
    body: "Volverá a verse en el catálogo público y sus productos estarán disponibles para la venta, si también se encuentran activos.",
    confirm: "Sí, activar",
    menuLabel: "Activar",
  },
};

function StatusBadge({ status }: { status: "ACTIVE" | "INACTIVE" }) {
  const active = status === "ACTIVE";
  return (
    <span
      className={cx(
        "inline-flex items-center gap-1.5 rounded-lg px-2 py-0.5 text-[11px] font-semibold",
        active ? "bg-emerald-50 text-emerald-700" : "bg-sf-bg text-sf-muted",
      )}
    >
      <span
        className={cx(
          "size-1.5 rounded-full",
          active ? "bg-emerald-500" : "bg-sf-muted",
        )}
      />
      {active ? "Activa" : "Inactiva"}
    </span>
  );
}

/** Menú ⋯ por fila: muestra la única transición válida del estado. */
function CategoryRowMenu({
  category,
  disabled,
  onSelect,
}: {
  category: Category;
  disabled: boolean;
  onSelect: (action: ConfirmAction) => void;
}) {
  const menuId = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const action: ConfirmAction =
    category.status === "ACTIVE" ? "deactivate" : "activate";

  useEffect(() => {
    if (!open) {
      return;
    }
    function onPointerDown(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        aria-label="Más acciones de la categoría"
        disabled={disabled}
        onClick={() => setOpen((value) => !value)}
        className={cx(
          "inline-flex h-8 w-8 items-center justify-center rounded-lg text-lg font-bold leading-none text-sf-muted transition-colors hover:bg-sf-bg hover:text-sf-ink",
          "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
          "disabled:cursor-not-allowed disabled:opacity-60",
        )}
      >
        ⋮
      </button>
      {open ? (
        <div
          id={menuId}
          role="menu"
          aria-label="Más acciones de la categoría"
          className="absolute right-0 top-full z-20 mt-1 w-44 overflow-hidden rounded-xl border border-sf-border bg-sf-surface py-1 shadow-[0_8px_24px_rgba(16,24,40,0.10)]"
        >
          <button
            type="button"
            role="menuitem"
            className={cx(
              "flex w-full min-h-9 items-center px-3 text-left text-sm transition-colors hover:bg-sf-bg",
              action === "deactivate" ? "text-sf-error" : "text-sf-ink",
            )}
            onClick={() => {
              setOpen(false);
              onSelect(action);
            }}
          >
            {CONFIRM_COPY[action].menuLabel}
          </button>
        </div>
      ) : null}
    </div>
  );
}

function ConfirmBanner({
  action,
  message,
  pending,
  onConfirm,
  onCancel,
}: {
  action: ConfirmAction;
  message: string | null;
  pending: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="border-t border-sf-border/80 bg-sf-bg/70 px-4 py-3 md:px-5">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="flex items-start gap-2.5">
          <InfoIcon className="mt-0.5 h-4 w-4 shrink-0 text-sf-primary" />
          <div>
            <p className="text-sm font-semibold text-sf-ink">
              {CONFIRM_COPY[action].title}
            </p>
            <p className="mt-0.5 text-xs text-sf-muted">
              {CONFIRM_COPY[action].body}
            </p>
            {message ? (
              <p className="mt-1 text-xs text-sf-error" role="alert">
                {message}
              </p>
            ) : null}
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <button
            type="button"
            disabled={pending}
            onClick={onConfirm}
            className={cx(
              "inline-flex min-h-8 items-center justify-center rounded-lg px-3.5 text-sm font-semibold transition-colors",
              action === "deactivate"
                ? "bg-sf-error text-white hover:opacity-90"
                : "bg-sf-primary text-white hover:bg-sf-primary-hover",
              "disabled:cursor-not-allowed disabled:opacity-60",
            )}
          >
            {pending ? "Guardando…" : CONFIRM_COPY[action].confirm}
          </button>
          <button
            type="button"
            disabled={pending}
            onClick={onCancel}
            className="inline-flex min-h-8 items-center justify-center rounded-lg border border-sf-border bg-sf-surface px-3.5 text-sm font-semibold text-sf-ink transition-colors hover:bg-sf-bg disabled:cursor-not-allowed disabled:opacity-60"
          >
            Cancelar
          </button>
        </div>
      </div>
    </div>
  );
}

export function AdminCategoriesPageContent() {
  const categoriesQuery = useAdminCategoriesQuery();
  const productsQuery = useAdminProductsQuery({});
  const activate = useActivateAdminCategoryMutation();
  const deactivate = useDeactivateAdminCategoryMutation();

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("");
  const [sortDir, setSortDir] = useState<SortDir>("asc");
  const [confirming, setConfirming] = useState<{
    id: string;
    action: ConfirmAction;
  } | null>(null);

  const pendingId = activate.isPending
    ? activate.variables
    : deactivate.isPending
      ? deactivate.variables
      : null;
  const confirmError = activate.error ?? deactivate.error;
  const confirmMessage = confirmError
    ? isApiError(confirmError)
      ? messageForApiProblem(confirmError.problem)
      : "No se pudo completar la solicitud."
    : null;

  function openConfirm(category: Category, action: ConfirmAction) {
    activate.reset();
    deactivate.reset();
    setConfirming({ id: category.id, action });
  }

  function runConfirm() {
    if (!confirming) {
      return;
    }
    const mutation =
      confirming.action === "deactivate" ? deactivate : activate;
    mutation.mutate(confirming.id, {
      onSuccess: () => setConfirming(null),
    });
  }

  /** Conteo real de productos por categoría: una sola descarga del listado
   *  admin completo, agregada client-side (sin N requests ni inventarios). */
  const productCountByCategory = useMemo(() => {
    const map = new Map<string, number>();
    for (const product of productsQuery.data ?? []) {
      map.set(product.categoryId, (map.get(product.categoryId) ?? 0) + 1);
    }
    return map;
  }, [productsQuery.data]);

  function productsLabel(categoryId: string): string {
    if (productsQuery.isPending) {
      return "…";
    }
    const count = productCountByCategory.get(categoryId) ?? 0;
    return count === 1 ? "1 producto" : `${count} productos`;
  }

  const visibleCategories = useMemo(() => {
    const query = search.trim().toLowerCase();
    return (categoriesQuery.data ?? [])
      .filter((category) =>
        statusFilter ? category.status === statusFilter : true,
      )
      .filter((category) =>
        query ? category.name.toLowerCase().includes(query) : true,
      )
      .sort((a, b) =>
        sortDir === "desc"
          ? b.name.localeCompare(a.name, "es")
          : a.name.localeCompare(b.name, "es"),
      );
  }, [categoriesQuery.data, search, statusFilter, sortDir]);

  return (
    /* Contenido ~12% más ancho que max-w-7xl: más aire para la tabla sin
       perder los márgenes laterales ni el fondo visible. */
    <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <span
            className={cx(
              "flex size-12 shrink-0 items-center justify-center rounded-xl",
              "bg-violet-100 text-violet-600",
            )}
          >
            <TagIcon className="h-6 w-6" />
          </span>
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-sf-ink">
              Categorías
            </h1>
            <p className="mt-0.5 max-w-2xl text-sm text-sf-muted">
              Gestiona las categorías del catálogo. El estado se cambia con
              activar o desactivar, no desde el formulario de edición.
            </p>
          </div>
        </div>
        <Link
          href="/admin/categories/new"
          className={cx(buttonClassName("primary"), "shrink-0")}
        >
          + Crear categoría
        </Link>
      </div>

      {categoriesQuery.isPending ? (
        <Card className="mt-6 overflow-hidden p-0">
          <div className="space-y-2 p-4" aria-hidden="true">
            {[0, 1, 2, 3, 4].map((row) => (
              <div key={row} className="flex items-center gap-3">
                <Skeleton className="h-10 w-10 shrink-0 rounded-lg" />
                <div className="min-w-0 flex-1 space-y-1.5">
                  <Skeleton className="h-4 w-1/3" />
                  <Skeleton className="h-3 w-1/2" />
                </div>
                <Skeleton className="hidden h-5 w-20 md:block" />
                <Skeleton className="hidden h-4 w-24 md:block" />
                <Skeleton className="hidden h-8 w-32 md:block" />
              </div>
            ))}
          </div>
        </Card>
      ) : null}

      {categoriesQuery.isError ? (
        <div className="mt-6 grid gap-3">
          <Alert tone="error" title="No se pudieron cargar las categorías">
            {isApiError(categoriesQuery.error)
              ? messageForApiProblem(categoriesQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => categoriesQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      ) : null}

      {categoriesQuery.isSuccess && categoriesQuery.data.length === 0 ? (
        <div className="mt-6">
          <EmptyState
            title="No hay categorías para mostrar."
            description="Crea la primera categoría para organizar el catálogo."
            action={
              <Link
                href="/admin/categories/new"
                className={buttonClassName("primary")}
              >
                Crear categoría
              </Link>
            }
          />
        </div>
      ) : null}

      {categoriesQuery.isSuccess &&
      categoriesQuery.data.length > 0 &&
      visibleCategories.length === 0 ? (
        <div className="mt-6">
          <EmptyState
            title="No encontramos categorías con estos filtros."
            description="Ajusta la búsqueda o el estado para ver más resultados."
          />
        </div>
      ) : null}

      {categoriesQuery.isSuccess && visibleCategories.length > 0 ? (
        <Card className="mt-6 overflow-hidden p-0 shadow-[0_1px_2px_rgba(16,24,40,0.05)]">
          <div className="flex flex-col gap-3 border-b border-sf-border px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex flex-wrap items-center gap-3">
              <label className="relative min-w-0 flex-1 sm:max-w-xs">
                <SearchIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-sf-muted" />
                <span className="sr-only">Buscar categorías</span>
                <input
                  type="search"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Buscar categorías..."
                  className="min-h-9 w-full rounded-lg border border-sf-border bg-sf-surface pl-9 pr-3 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary"
                />
              </label>
              <div className="w-48">
                <SelectField
                  id="admin-category-status"
                  label="Estado"
                  labelClassName="sr-only"
                  className="w-full min-w-0"
                  value={statusFilter}
                  onChange={(event) =>
                    setStatusFilter(event.target.value as StatusFilter)
                  }
                >
                  <option value="">Todos los estados</option>
                  <option value="ACTIVE">Activas</option>
                  <option value="INACTIVE">Inactivas</option>
                </SelectField>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <span className="whitespace-nowrap text-sm text-sf-muted">
                Ordenar por
              </span>
              <div className="w-44">
                <SelectField
                  id="admin-category-sort"
                  label="Ordenar por"
                  labelClassName="sr-only"
                  className="w-full min-w-0"
                  value={sortDir}
                  onChange={(event) => setSortDir(event.target.value as SortDir)}
                >
                  <option value="asc">Nombre (A - Z)</option>
                  <option value="desc">Nombre (Z - A)</option>
                </SelectField>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3 border-b border-sf-border bg-sf-bg/60 px-4 py-2 text-xs font-medium text-sf-muted">
            <span className="min-w-0 flex-1">Nombre</span>
            <span className="w-[26rem] shrink-0">Descripción</span>
            <span className="w-24 shrink-0 text-center">Estado</span>
            <span className="w-32 shrink-0 text-right">Productos</span>
            <span className="w-40 shrink-0 text-right">Acciones</span>
          </div>
          <ul className="divide-y divide-sf-border/80">
            {visibleCategories.map((category) => (
              <li key={category.id}>
                <div
                  className={cx(
                    "flex items-center gap-3 px-4 py-2.5 transition-colors duration-150",
                    confirming?.id === category.id
                      ? "bg-sf-bg/80"
                      : "hover:bg-sf-bg/60",
                  )}
                >
                  <CategoryIcon icon={category.icon} />
                  <div className="min-w-0 flex-1">
                    <p
                      className="truncate text-sm font-semibold text-sf-ink"
                      title={category.name}
                    >
                      {category.name}
                    </p>
                  </div>
                  <p
                    className="hidden w-[26rem] shrink-0 truncate text-sm text-sf-muted md:block"
                    title={category.description ?? undefined}
                  >
                    {category.description?.trim() || "—"}
                  </p>
                  <span className="flex w-24 shrink-0 justify-center">
                    <StatusBadge status={category.status} />
                  </span>
                  <span className="hidden w-32 shrink-0 text-right text-sm text-sf-muted sm:block">
                    {productsLabel(category.id)}
                  </span>
                  <span className="flex w-40 shrink-0 items-center justify-end gap-1.5">
                    <Link
                      href={`/admin/categories/${category.id}`}
                      className={cx(
                        buttonClassName("secondary"),
                        "min-h-8 gap-1.5 rounded-lg px-3 text-sm",
                      )}
                    >
                      Ver y editar
                    </Link>
                    <CategoryRowMenu
                      category={category}
                      disabled={pendingId != null}
                      onSelect={(action) => openConfirm(category, action)}
                    />
                  </span>
                </div>
                {confirming?.id === category.id ? (
                  <ConfirmBanner
                    action={confirming.action}
                    message={confirmMessage}
                    pending={pendingId != null}
                    onConfirm={runConfirm}
                    onCancel={() => setConfirming(null)}
                  />
                ) : null}
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
    </main>
  );
}
