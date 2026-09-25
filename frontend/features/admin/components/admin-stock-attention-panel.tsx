"use client";

import { useMemo } from "react";
import Link from "next/link";
import type { AdminProduct } from "@/features/admin/api";
import { HubStockEmptyArt } from "@/features/admin/components/admin-hub-art";
import {
  useAdminCategoriesQuery,
  useAdminProductsQuery,
} from "@/features/admin/hooks";
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
import {
  CheckCircleIcon,
  PackageIcon,
  WarningIcon,
} from "@/shared/ui/icons";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

type StockSectionKind = "low" | "out";

export function AdminStockAttentionPanel() {
  const productsQuery = useAdminProductsQuery({ status: "ACTIVE" });
  const categoriesQuery = useAdminCategoriesQuery();

  const categoriesById = useMemo(() => {
    const map = new Map<string, string>();
    for (const category of categoriesQuery.data ?? []) {
      map.set(category.id, category.name);
    }
    return map;
  }, [categoriesQuery.data]);

  const buckets = useMemo(
    () => partitionAdminStockAttention(productsQuery.data ?? []),
    [productsQuery.data],
  );

  const loading = productsQuery.isPending || categoriesQuery.isPending;
  const failed = productsQuery.isError;

  return (
    <section
      className="mt-10 md:mt-12"
      aria-labelledby="admin-stock-heading"
    >
      <div
        className={cx(
          "rounded-3xl border border-sf-border/90 bg-sf-bg/90 p-4",
          "shadow-[0_2px_10px_rgba(23,33,27,0.04)] md:p-6",
        )}
      >
        <div className="mb-5 flex items-center gap-3 px-1">
          <span
            className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-amber-100 text-amber-800 ring-1 ring-amber-200/80"
            aria-hidden="true"
          >
            <PackageIcon className="h-6 w-6" />
          </span>
          <h2
            id="admin-stock-heading"
            className="text-xl font-bold tracking-tight text-sf-ink md:text-2xl"
          >
            Control de inventario
          </h2>
        </div>

        {loading ? <StockPanelSkeleton /> : null}

        {failed ? (
          <Alert tone="error" title="No se pudieron cargar los productos.">
            {isApiError(productsQuery.error)
              ? messageForApiProblem(productsQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
        ) : null}

        {!loading && !failed ? (
          <div className="grid gap-4 md:grid-cols-2">
            <StockAttentionSection
              kind="low"
              title="Quedan pocas unidades"
              emptyTitle="¡Todo bajo control!"
              emptyMessage={ADMIN_STOCK_LOW_EMPTY_MESSAGE}
              products={buckets.lowStock}
              categoriesById={categoriesById}
            />
            <StockAttentionSection
              kind="out"
              title="Agotados"
              emptyTitle="Sin productos agotados"
              emptyMessage={ADMIN_STOCK_OUT_EMPTY_MESSAGE}
              products={buckets.outOfStock}
              categoriesById={categoriesById}
            />
          </div>
        ) : null}
      </div>
    </section>
  );
}

function StockAttentionSection({
  kind,
  title,
  emptyTitle,
  emptyMessage,
  products,
  categoriesById,
}: {
  kind: StockSectionKind;
  title: string;
  emptyTitle: string;
  emptyMessage: string;
  products: AdminProduct[];
  categoriesById: Map<string, string>;
}) {
  const isLow = kind === "low";
  const countLabel =
    products.length === 1 ? "1 producto" : `${products.length} productos`;

  return (
    <Card
      className={cx(
        "overflow-hidden p-0 transition-shadow duration-200 hover:shadow-[0_8px_20px_rgba(23,33,27,0.06)]",
        isLow
          ? "border-amber-200/90 bg-amber-50/40 shadow-[0_1px_2px_rgba(181,71,8,0.05)]"
          : "border-red-200/90 bg-red-50/35 shadow-[0_1px_2px_rgba(217,45,32,0.05)]",
      )}
    >
      <div
        className={cx(
          "flex items-center justify-between gap-3 border-b px-4 py-4 md:px-5",
          isLow
            ? "border-amber-100/90 bg-amber-50/80"
            : "border-red-100/90 bg-red-50/70",
        )}
      >
        <div className="flex min-w-0 items-center gap-3">
          <span
            className={cx(
              "flex h-10 w-10 shrink-0 items-center justify-center rounded-xl",
              isLow
                ? "bg-amber-100 text-amber-700"
                : "bg-red-100 text-red-700",
            )}
            aria-hidden="true"
          >
            {isLow ? (
              <WarningIcon className="h-5 w-5" />
            ) : (
              <PackageIcon className="h-5 w-5" />
            )}
          </span>
          <h3 className="truncate text-base font-semibold text-sf-ink md:text-lg">
            {title}
          </h3>
        </div>
        <span
          className={cx(
            "inline-flex shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold tabular-nums",
            isLow
              ? "bg-amber-100 text-amber-800"
              : "bg-red-100 text-red-800",
          )}
        >
          {countLabel}
        </span>
      </div>

      {products.length === 0 ? (
        <StockEmptyState
          kind={kind}
          title={emptyTitle}
          message={emptyMessage}
        />
      ) : (
        <ul className="divide-y divide-sf-border/70 bg-sf-surface/90">
          {products.map((product) => (
            <StockAttentionRow
              key={product.id}
              product={product}
              categoryName={categoriesById.get(product.categoryId)}
              kind={kind}
            />
          ))}
        </ul>
      )}
    </Card>
  );
}

function StockEmptyState({
  kind,
  title,
  message,
}: {
  kind: StockSectionKind;
  title: string;
  message: string;
}) {
  const isLow = kind === "low";
  return (
    <div
      className={cx(
        "flex min-h-[16rem] flex-col items-center justify-center px-5 py-10 text-center md:min-h-[17rem] md:px-8",
        isLow ? "bg-amber-50/30" : "bg-red-50/20",
      )}
    >
      {isLow ? (
        <span
          className="mb-1 h-[5.5rem] w-[6.5rem] text-amber-700/80 md:h-24 md:w-[7.25rem]"
          aria-hidden="true"
        >
          <HubStockEmptyArt />
        </span>
      ) : (
        <span
          className="relative mb-1 flex h-16 w-16 items-center justify-center rounded-2xl bg-sf-surface text-sf-primary shadow-sm ring-1 ring-sf-border"
          aria-hidden="true"
        >
          <PackageIcon className="h-8 w-8 opacity-80" />
          <span className="absolute -bottom-1 -right-1 flex h-7 w-7 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 ring-2 ring-sf-surface">
            <CheckCircleIcon className="h-3.5 w-3.5" />
          </span>
        </span>
      )}
      <p className="mt-4 text-lg font-bold tracking-tight text-sf-ink">{title}</p>
      <p className="mt-1.5 max-w-sm text-sm leading-relaxed text-sf-muted">
        {message}
      </p>
    </div>
  );
}

function StockAttentionRow({
  product,
  categoryName,
  kind,
}: {
  product: AdminProduct;
  categoryName: string | undefined;
  kind: StockSectionKind;
}) {
  const isLow = kind === "low";
  const href = adminProductDetailHref(product.id);
  const presentation = formatAdminPresentation(product.presentation);
  const meta = [product.brand, presentation, categoryName]
    .filter((part): part is string => Boolean(part && part.trim()))
    .join(" · ");

  return (
    <li>
      <div
        className={cx(
          "grid gap-3 px-4 py-3.5 transition-colors duration-150 hover:bg-sf-bg/90 md:px-5 md:items-center md:gap-4",
          isLow
            ? "md:grid-cols-[3.25rem_minmax(0,1fr)_auto_auto]"
            : "md:grid-cols-[3.25rem_minmax(0,1fr)_auto]",
        )}
      >
        <div className="flex min-w-0 items-start gap-3 md:contents">
          <Link
            href={href}
            className="block w-14 shrink-0 overflow-hidden rounded-xl ring-1 ring-sf-border/80 transition-transform duration-150 hover:scale-[1.02] md:w-[3.25rem]"
            aria-label={product.name}
          >
            <ProductImage
              src={product.imageUrl}
              alt=""
              className="aspect-square w-full"
            />
          </Link>

          <div className="min-w-0 flex-1 md:contents">
            <div className="min-w-0 md:block">
              <Link
                href={href}
                className="line-clamp-2 text-base font-semibold text-sf-ink transition-colors hover:text-sf-primary"
              >
                {product.name}
              </Link>
              {meta ? (
                <p className="mt-0.5 line-clamp-2 text-sm text-sf-muted md:truncate">
                  {meta}
                </p>
              ) : null}
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 md:contents">
          {isLow ? (
            <div className="inline-flex items-center rounded-full bg-amber-100 px-3 py-1.5 text-xs font-bold uppercase tracking-wide text-amber-800 md:justify-self-end">
              Stock {product.stock}
            </div>
          ) : null}

          <div className="md:justify-self-end">
            <Link
              href={href}
              className={buttonClassName(
                "secondary",
                cx(
                  "min-h-9 w-full min-w-[7.25rem] px-3 py-1.5 text-xs font-semibold",
                  "border-sf-border bg-sf-bg text-sf-ink",
                  "hover:border-sf-border hover:bg-sf-surface",
                  "transition-colors duration-150 sm:w-auto",
                ),
              )}
            >
              Ver producto
            </Link>
          </div>
        </div>
      </div>
    </li>
  );
}

function StockPanelSkeleton() {
  return (
    <div className="grid gap-4 md:grid-cols-2" aria-hidden="true">
      {[0, 1].map((section) => (
        <Card key={section} className="grid gap-3 p-4 md:p-5">
          <div className="flex items-center gap-3">
            <Skeleton className="h-10 w-10 rounded-xl" />
            <div className="grid flex-1 gap-2">
              <Skeleton className="h-5 w-48" />
              <Skeleton className="h-4 w-24 max-w-full" />
            </div>
          </div>
          <Skeleton className="h-28 w-full rounded-xl" />
        </Card>
      ))}
    </div>
  );
}
