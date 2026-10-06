"use client";

import Link from "next/link";
import { useAdminCategoriesQuery, useAdminProductsQuery } from "@/features/admin/hooks";
import {
  ADMIN_STOCK_LOW_EMPTY_MESSAGE,
  ADMIN_STOCK_OUT_EMPTY_MESSAGE,
  adminProductDetailHref,
  formatAdminPresentation,
  partitionAdminStockAttention,
} from "@/features/admin/presentation";
import { ProductImage } from "@/features/catalog/components/product-image";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { EmptyState } from "@/shared/ui/empty-state";
import { PackageIcon, WarningIcon } from "@/shared/ui/icons";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

export type AdminStockFocusViewKind = "low" | "out";

const COPY: Record<
  AdminStockFocusViewKind,
  {
    title: string;
    emptyTitle: string;
    emptyMessage: string;
    subtitle: (count: number) => string;
    stockLabel: string;
  }
> = {
  out: {
    title: "Productos agotados",
    emptyTitle: "No hay productos agotados",
    emptyMessage: ADMIN_STOCK_OUT_EMPTY_MESSAGE,
    subtitle: (count) =>
      count === 1
        ? "1 producto requiere reposición inmediata"
        : `${count} productos requieren reposición inmediata`,
    stockLabel: "Stock",
  },
  low: {
    title: "Próximos a agotarse",
    emptyTitle: "No hay productos próximos a agotarse",
    emptyMessage: ADMIN_STOCK_LOW_EMPTY_MESSAGE,
    subtitle: (count) =>
      count === 1
        ? "1 producto con existencias bajas"
        : `${count} productos con existencias bajas`,
    stockLabel: "Stock",
  },
};

/**
 * Vista dedicada de Control de inventario para una sola atención
 * ("out" = agotados, "low" = próximos a agotarse). Solo presentación:
 * los productos y su partición provienen de la lógica existente.
 */
export function AdminStockFocusView({ focus }: { focus: AdminStockFocusViewKind }) {
  const copy = COPY[focus];
  const productsQuery = useAdminProductsQuery({ status: "ACTIVE" });
  const categoriesQuery = useAdminCategoriesQuery();

  const categoriesById = new Map<string, string>();
  for (const category of categoriesQuery.data ?? []) {
    categoriesById.set(category.id, category.name);
  }

  const products = (focus === "low"
    ? [...partitionAdminStockAttention(productsQuery.data ?? []).lowStock]
    : [...partitionAdminStockAttention(productsQuery.data ?? []).outOfStock]
  ).sort((a, b) => a.stock - b.stock);

  const isOut = focus === "out";

  return (
    <Card className="mt-5 overflow-hidden p-0">
      <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-4 md:px-5">
        <div className="flex items-center gap-3">
          <span
            className={cx(
              "inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg",
              isOut ? "bg-red-100 text-red-700" : "bg-amber-100 text-amber-800",
            )}
            aria-hidden="true"
          >
            {isOut ? <PackageIcon className="h-5 w-5" /> : <WarningIcon className="h-5 w-5" />}
          </span>
          <h2 className="text-lg font-semibold text-sf-ink">{copy.title}</h2>
        </div>
        {productsQuery.isPending ? (
          <Skeleton className="h-6 w-24" />
        ) : (
          <span
            className={cx(
              "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold tabular-nums",
              isOut ? "bg-red-50 text-red-700" : "bg-amber-50 text-amber-800",
            )}
          >
            {products.length === 1 ? "1 producto" : `${products.length} productos`}
          </span>
        )}
      </div>

      <p className="-mt-2 px-4 pb-3 text-xs text-sf-muted md:px-5">
        {productsQuery.isPending
          ? null
          : copy.subtitle(products.length)}
      </p>

      {productsQuery.isPending ? (
        <StockFocusSkeleton />
      ) : productsQuery.isError ? (
        <div className="px-4 pb-4 md:px-5">
          <Alert tone="error" title="No se pudieron cargar los productos">
            {isApiError(productsQuery.error)
              ? messageForApiProblem(productsQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
          <div className="mt-3">
            <button
              type="button"
              className={buttonClassName("secondary", "min-h-9 px-4 text-sm")}
              onClick={() => void productsQuery.refetch()}
            >
              Reintentar
            </button>
          </div>
        </div>
      ) : products.length === 0 ? (
        <div className="px-4 pb-5 md:px-5">
          <EmptyState title={copy.emptyTitle} description={copy.emptyMessage} />
        </div>
      ) : (
        <>
          <div className="hidden overflow-x-auto lg:block">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-y border-sf-border bg-sf-bg/60 text-xs font-medium text-sf-muted">
                  <th className="px-4 py-3 font-medium">Producto</th>
                  <th className="px-4 py-3 font-medium">Categoría</th>
                  <th className="px-4 py-3 text-right font-medium">Stock</th>
                  {isOut ? (
                    <th className="px-4 py-3 font-medium">Estado</th>
                  ) : null}
                  <th className="px-4 py-3 text-right font-medium">
                    <span className="sr-only">Acción</span>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-sf-border/80">
                {products.map((product) => (
                  <tr
                    key={product.id}
                    className="transition-colors duration-150 hover:bg-sf-bg/60"
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <Link
                          href={adminProductDetailHref(product.id)}
                          className="block h-10 w-10 shrink-0 overflow-hidden rounded-lg ring-1 ring-sf-border/80"
                          aria-label={product.name}
                          tabIndex={-1}
                        >
                          <ProductImage
                            src={product.imageUrl}
                            alt=""
                            className="aspect-square w-full"
                          />
                        </Link>
                        <div className="min-w-0">
                          <Link
                            href={adminProductDetailHref(product.id)}
                            className="font-semibold text-sf-ink transition-colors hover:text-sf-primary"
                          >
                            {product.name}
                          </Link>
                          <p className="mt-0.5 text-xs text-sf-muted">
                            {[product.brand, formatAdminPresentation(product.presentation)]
                              .filter((part): part is string => Boolean(part && part.trim()))
                              .join(" · ") || "—"}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sf-muted">
                      {categoriesById.get(product.categoryId) ?? "—"}
                    </td>
                    <StockCell focus={focus} stock={product.stock} />
                    {isOut ? (
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center rounded-lg bg-red-50 px-2 py-0.5 text-[11px] font-semibold text-red-700">
                          Agotado
                        </span>
                      </td>
                    ) : null}
                    <td className="px-4 py-3 text-right">
                      <Link
                        href={adminProductDetailHref(product.id)}
                        className={cx(
                          buttonClassName("secondary"),
                          "min-h-8 rounded-lg px-3 text-sm",
                        )}
                      >
                        Ver detalle
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="grid gap-2 px-4 pb-4 lg:hidden">
            {products.map((product) => (
              <li key={product.id}>
                <div className="rounded-xl border border-sf-border bg-sf-surface p-3.5">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex min-w-0 items-center gap-3">
                      <Link
                        href={adminProductDetailHref(product.id)}
                        className="block h-11 w-11 shrink-0 overflow-hidden rounded-lg ring-1 ring-sf-border/80"
                        aria-label={product.name}
                        tabIndex={-1}
                      >
                        <ProductImage
                          src={product.imageUrl}
                          alt=""
                          className="aspect-square w-full"
                        />
                      </Link>
                      <div className="min-w-0">
                        <Link
                          href={adminProductDetailHref(product.id)}
                          className="line-clamp-2 text-sm font-semibold text-sf-ink"
                        >
                          {product.name}
                        </Link>
                        <p className="mt-0.5 text-xs text-sf-muted">
                          {categoriesById.get(product.categoryId) ?? "—"}
                        </p>
                      </div>
                    </div>
                    <StockValue focus={focus} stock={product.stock} compact />
                  </div>
                  <div className="mt-3 flex items-center justify-between gap-3">
                    <span className="text-xs text-sf-muted">
                      {[product.brand, formatAdminPresentation(product.presentation)]
                        .filter((part): part is string => Boolean(part && part.trim()))
                        .join(" · ") || "\u00a0"}
                    </span>
                    {isOut ? (
                      <span className="inline-flex items-center rounded-lg bg-red-50 px-2 py-0.5 text-[11px] font-semibold text-red-700">
                        Agotado
                      </span>
                    ) : null}
                  </div>
                  <div className="mt-3 flex justify-end">
                    <Link
                      href={adminProductDetailHref(product.id)}
                      className={cx(
                        buttonClassName("secondary"),
                        "min-h-8 rounded-lg px-3 text-sm",
                      )}
                    >
                      Ver detalle
                    </Link>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}
    </Card>
  );
}

/** Presentación del valor de stock, válida en cualquier contenedor
 *  (span): la usan tanto la celda de tabla como la tarjeta mobile. */
function StockValue({
  focus,
  stock,
  compact = false,
}: {
  focus: AdminStockFocusViewKind;
  stock: number;
  compact?: boolean;
}) {
  if (focus === "out") {
    return (
      <span
        className={cx(
          "font-bold tabular-nums text-red-700",
          compact ? "text-lg" : "text-base",
        )}
      >
        {stock}
      </span>
    );
  }
  return (
    <span
      className={cx(
        "inline-flex items-center rounded-lg bg-amber-50 px-2.5 py-1 font-bold tabular-nums text-amber-800",
        compact ? "text-sm" : "text-base",
      )}
      aria-label={`Stock ${stock} unidades`}
    >
      {stock}
    </span>
  );
}

/** Celda de tabla: envuelve StockValue en un <td> válido. */
function StockCell({
  focus,
  stock,
}: {
  focus: AdminStockFocusViewKind;
  stock: number;
}) {
  return (
    <td className="px-4 py-3 text-right">
      <StockValue focus={focus} stock={stock} />
    </td>
  );
}

function StockFocusSkeleton() {
  return (
    <div className="grid gap-2 px-4 pb-4 md:px-5" aria-hidden="true">
      {[0, 1, 2, 3, 4].map((row) => (
        <div
          key={row}
          className="flex items-center gap-4 rounded-xl border border-sf-border bg-sf-surface px-4 py-3"
        >
          <Skeleton className="h-10 w-10 rounded-lg" />
          <div className="min-w-0 flex-1 space-y-2">
            <Skeleton className="h-4 w-44" />
            <Skeleton className="h-3 w-24" />
          </div>
          <Skeleton className="h-6 w-12" />
          <Skeleton className="h-8 w-24" />
        </div>
      ))}
    </div>
  );
}
