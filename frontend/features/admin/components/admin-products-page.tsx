"use client";

/* Lista de productos Admin: filas compactas con miniatura (ProductImage),
 * identidad agrupada (nombre + marca), acciones [Editar] [⋯]. */

import { useMemo, type FormEvent } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ProductStatusActions } from "@/features/admin/components/product-status-actions";
import { ProductStatusBadge } from "@/features/admin/components/product-status-badge";
import {
  useAdminCategoriesQuery,
  useAdminProductsQuery,
} from "@/features/admin/hooks";
import {
  ADMIN_STOCK_LOW_MAX,
  adminProductDetailHref,
  adminProductsHref,
  listQueryFromSearchParams,
  mergeAdminProductsFilters,
  shouldSearchAdminProducts,
} from "@/features/admin/presentation";
import { productAvailabilityLabel } from "@/features/catalog/quantity";
import { ProductImage } from "@/features/catalog/components/product-image";
import type { Product, ProductStatus } from "@/features/catalog/api";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { EmptyState } from "@/shared/ui/empty-state";
import { SelectField } from "@/shared/ui/select-field";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

/** Etiqueta de stock con las reglas reales del proyecto (umbral "Bajo"
 *  reutiliza ADMIN_STOCK_LOW_MAX del dominio admin). */
function stockStatusLabel(product: Product): string {
  if (product.status !== "ACTIVE") {
    return productAvailabilityLabel(product.status, product.stock);
  }
  if (product.stock <= 0) {
    return "Agotado";
  }
  if (product.stock <= ADMIN_STOCK_LOW_MAX) {
    return "Bajo";
  }
  return "Disponible";
}

/** Semántica cromática del stock (tokens existentes: verde/ámbar/rojo). */
function stockToneClass(label: string): string {
  switch (label) {
    case "Disponible":
      return "bg-emerald-50 text-emerald-700";
    case "Bajo":
      return "bg-amber-50 text-amber-700";
    case "Agotado":
      return "bg-red-50 text-red-700";
    default:
      return "bg-sf-bg text-sf-muted";
  }
}

function StockBadge({ product }: { product: Product }) {
  const label = stockStatusLabel(product);
  return (
    <span
      className={cx(
        "inline-flex items-center rounded-lg px-2 py-0.5 text-[11px] font-semibold",
        stockToneClass(label),
      )}
    >
      {label}
    </span>
  );
}

function ProductThumb({ product }: { product: Product }) {
  return (
    <div className="h-12 w-12 shrink-0 overflow-hidden rounded-lg border border-sf-border bg-sf-surface">
      <ProductImage
        src={product.imageUrl}
        alt={product.name}
        compact
        className="h-full w-full"
      />
    </div>
  );
}

function ProductIdentity({
  product,
}: {
  product: Product;
}) {
  return (
    <div className="min-w-0">
      <p className="truncate text-sm font-semibold text-sf-ink" title={product.name}>
        {product.name}
      </p>
      <p className="truncate text-xs text-sf-muted">
        {product.brand ?? "Sin marca"}
      </p>
    </div>
  );
}

function editHref(product: Product, context: ProductListContext): string {
  return adminProductDetailHref(product.id, {
    text: context.text,
    categoryId: context.categoryId,
    status: context.status,
  });
}

type ProductListContext = {
  text: string;
  categoryId?: string;
  status?: ProductStatus;
};

export function AdminProductsPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const text = searchParams.get("text") ?? "";
  const searching = shouldSearchAdminProducts(text);
  const listQuery = listQueryFromSearchParams({
    categoryId: searchParams.get("categoryId") ?? undefined,
    status: searchParams.get("status") ?? undefined,
  });
  const productsQuery = useAdminProductsQuery({
    text,
    categoryId: listQuery.categoryId,
    status: listQuery.status,
  });
  const categoriesQuery = useAdminCategoriesQuery();
  const categoriesById = useMemo(() => {
    const map = new Map<string, string>();
    for (const category of categoriesQuery.data ?? []) {
      map.set(category.id, category.name);
    }
    return map;
  }, [categoriesQuery.data]);
  const listContext: ProductListContext = {
    text,
    categoryId: listQuery.categoryId,
    status: listQuery.status,
  };

  function replaceFilters(
    next: Parameters<typeof mergeAdminProductsFilters>[1],
  ) {
    router.replace(
      adminProductsHref(
        mergeAdminProductsFilters(
          {
            text,
            categoryId: listQuery.categoryId,
            status: listQuery.status,
          },
          next,
        ),
      ),
    );
  }

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const submitted = String(
      new FormData(event.currentTarget).get("text") ?? "",
    ).trim();
    router.replace(
      adminProductsHref({
        text: submitted,
        categoryId: listQuery.categoryId,
        status: listQuery.status,
      }),
    );
  }

  const categoryOf = (product: Product) =>
    categoriesById.get(product.categoryId) ?? "—";

  return (
    /* Ancho moderadamente mayor que Container por defecto (max-w-6xl → 7xl). */
    <main className="mx-auto w-full max-w-7xl px-4 py-6 md:px-8 md:py-8">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-sf-ink">
          Productos
        </h1>
        <Link href="/admin/products/new" className={buttonClassName("primary")}>
          Crear producto
        </Link>
      </div>

      <form
        className="mt-6 flex flex-col gap-2 sm:flex-row"
        role="search"
        onSubmit={onSearch}
      >
        <label htmlFor="admin-product-search" className="sr-only">
          Buscar productos
        </label>
        <input
          id="admin-product-search"
          type="search"
          name="text"
          defaultValue={text}
          placeholder="Buscar por nombre, marca o código"
          className="min-h-9 flex-1 rounded-lg border border-sf-border bg-sf-surface px-3 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary"
        />
        <Button type="submit" className="min-h-9 px-4 text-sm">
          Buscar
        </Button>
      </form>

      <div className="mt-3 flex flex-wrap items-end gap-3">
        <div className="w-56">
          <SelectField
            id="admin-product-category"
            label="Categoría"
            labelClassName="text-xs font-medium text-sf-muted"
            className="w-full min-w-0 text-ellipsis"
            value={searching ? "" : (listQuery.categoryId ?? "")}
            disabled={searching}
            onChange={(event) =>
              replaceFilters({ categoryId: event.target.value || null })
            }
          >
            <option value="">Todas</option>
            {(categoriesQuery.data ?? []).map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </SelectField>
        </div>
        <div className="w-48">
          <SelectField
            id="admin-product-status"
            label="Estado"
            labelClassName="text-xs font-medium text-sf-muted"
            className="w-full min-w-0 text-ellipsis"
            value={searching ? "" : (listQuery.status ?? "")}
            disabled={searching}
            onChange={(event) =>
              replaceFilters({
                status: (event.target.value || "") as ProductStatus | "",
              })
            }
          >
            <option value="">Todos</option>
            <option value="ACTIVE">Activo</option>
            <option value="INACTIVE">Inactivo</option>
            <option value="ARCHIVED">Archivado</option>
          </SelectField>
        </div>
        {searching ? (
          <p className="text-xs text-sf-muted">
            La búsqueda usa el catálogo del servidor. Quita el texto para
            filtrar por categoría o estado.
          </p>
        ) : null}
      </div>

      {productsQuery.isPending ? (
        <div className="mt-6 grid gap-2">
          {[0, 1, 2, 3, 4].map((row) => (
            <div
              key={row}
              className="flex items-center gap-3 rounded-xl border border-sf-border bg-sf-surface px-4 py-2.5"
              aria-hidden="true"
            >
              <Skeleton className="h-12 w-12 shrink-0 rounded-lg" />
              <div className="min-w-0 flex-1 space-y-1.5">
                <Skeleton className="h-4 w-1/3" />
                <Skeleton className="h-3 w-1/4" />
              </div>
              <Skeleton className="hidden h-4 w-24 md:block" />
              <Skeleton className="hidden h-4 w-20 md:block" />
              <Skeleton className="hidden h-5 w-16 md:block" />
              <Skeleton className="hidden h-5 w-20 sm:block" />
            </div>
          ))}
        </div>
      ) : null}

      {productsQuery.isError ? (
        <div className="mt-6">
          <Alert tone="error" title="No se pudo cargar el catálogo">
            {isApiError(productsQuery.error)
              ? messageForApiProblem(productsQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
        </div>
      ) : null}

      {productsQuery.isSuccess && productsQuery.data.length === 0 ? (
        <div className="mt-6">
          <EmptyState
            title="No hay productos para mostrar"
            description={
              searching
                ? "Prueba con otro término o limpia la búsqueda."
                : "Crea el primer producto para el catálogo."
            }
            action={
              searching ? (
                <Link
                  href="/admin/products"
                  className={buttonClassName("secondary")}
                >
                  Ver todos
                </Link>
              ) : (
                <Link
                  href="/admin/products/new"
                  className={buttonClassName("primary")}
                >
                  Crear producto
                </Link>
              )
            }
          />
        </div>
      ) : null}

      {productsQuery.isSuccess && productsQuery.data.length > 0 ? (
        <>
          {/* Mobile / tablet: cards compactas con la misma información */}
          <ul className="mt-6 grid gap-2 lg:hidden">
            {productsQuery.data.map((product) => (
              <li key={product.id}>
                <Card className="p-3">
                  <div className="flex gap-3">
                    <ProductThumb product={product} />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p
                            className="truncate text-sm font-semibold text-sf-ink"
                            title={product.name}
                          >
                            {product.name}
                          </p>
                          <p className="truncate text-xs text-sf-muted">
                            {product.brand ?? "Sin marca"}
                          </p>
                        </div>
                        <ProductStatusBadge status={product.status} />
                      </div>
                      <p className="mt-1 truncate text-xs text-sf-muted">
                        {categoryOf(product)}
                      </p>
                      <div className="mt-1.5 flex items-center justify-between gap-2">
                        <span className="text-sm font-semibold tabular-nums text-sf-ink">
                          {formatMoney(product.price)}
                        </span>
                        <span className="flex items-center gap-1.5 text-xs text-sf-muted">
                          Stock: {product.stock}
                          <StockBadge product={product} />
                        </span>
                      </div>
                    </div>
                  </div>
                  <div className="mt-3 flex items-center justify-end gap-2">
                    <Link
                      href={editHref(product, listContext)}
                      className={cx(
                        buttonClassName("secondary"),
                        "min-h-8 rounded-lg px-3 text-sm",
                      )}
                    >
                      Editar
                    </Link>
                    <ProductStatusActions product={product} variant="menu" />
                  </div>
                </Card>
              </li>
            ))}
          </ul>

          {/* Desktop: filas administrativas compactas */}
          <Card className="mt-6 hidden overflow-hidden p-0 lg:block">
            <div className="flex items-center gap-3 border-b border-sf-border px-4 py-2 text-xs font-medium text-sf-muted">
              <span className="w-12 shrink-0" aria-hidden="true" />
              <span className="min-w-0 flex-[1.6]">Producto</span>
              <span className="w-44 shrink-0">Categoría</span>
              <span className="w-32 shrink-0 text-right">Precio</span>
              <span className="w-20 shrink-0 text-right">Stock</span>
              <span className="w-24 shrink-0 text-center">Estado</span>
              <span className="w-32 shrink-0 text-right">Acciones</span>
            </div>
            <ul className="divide-y divide-sf-border/80">
              {productsQuery.data.map((product) => (
                <li
                  key={product.id}
                  className="flex items-center gap-3 px-4 py-2.5 transition-colors duration-150 hover:bg-sf-bg/60"
                >
                  <ProductThumb product={product} />
                  <div className="min-w-0 flex-[1.6]">
                    <ProductIdentity product={product} />
                  </div>
                  <span
                    className="w-44 shrink-0 truncate text-sm text-sf-muted"
                    title={categoryOf(product)}
                  >
                    {categoryOf(product)}
                  </span>
                  <span className="w-32 shrink-0 text-right text-sm font-semibold tabular-nums text-sf-ink">
                    {formatMoney(product.price)}
                  </span>
                  <span className="w-20 shrink-0 text-right">
                    <span className="block text-sm font-bold tabular-nums text-sf-ink">
                      {product.stock}
                    </span>
                    <span className="mt-0.5 block">
                      <StockBadge product={product} />
                    </span>
                  </span>
                  <span className="flex w-24 shrink-0 justify-center">
                    <ProductStatusBadge status={product.status} />
                  </span>
                  <span className="flex w-32 shrink-0 justify-end">
                    <div className="flex items-center justify-end gap-1.5">
                      <Link
                        href={editHref(product, listContext)}
                        className={cx(
                          buttonClassName("secondary"),
                          "min-h-8 rounded-lg px-3 text-sm",
                        )}
                      >
                        Editar
                      </Link>
                      <ProductStatusActions product={product} variant="menu" />
                    </div>
                  </span>
                </li>
              ))}
            </ul>
          </Card>
        </>
      ) : null}
    </main>
  );
}