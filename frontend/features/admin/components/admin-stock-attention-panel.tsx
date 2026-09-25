"use client";

import { useMemo } from "react";
import Link from "next/link";
import type { AdminProduct } from "@/features/admin/api";
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
import { PackageIcon, WarningIcon } from "@/shared/ui/icons";
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
    <section className="mt-10 grid gap-6" aria-labelledby="admin-stock-heading">
      <div>
        <h2
          id="admin-stock-heading"
          className="text-xl font-bold tracking-tight text-sf-ink md:text-2xl"
        >
          Stock
        </h2>
        <p className="mt-1 max-w-2xl text-sm text-sf-muted md:text-base">
          Productos activos que necesitan reposición. Los inactivos y archivados
          no aparecen aquí.
        </p>
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
        <div className="grid gap-5">
          <StockAttentionSection
            kind="low"
            title="Próximos a agotarse"
            description="Activos con stock entre 1 y 5 unidades."
            emptyMessage={ADMIN_STOCK_LOW_EMPTY_MESSAGE}
            products={buckets.lowStock}
            categoriesById={categoriesById}
          />
          <StockAttentionSection
            kind="out"
            title="Agotados"
            description="Activos con stock en cero."
            emptyMessage={ADMIN_STOCK_OUT_EMPTY_MESSAGE}
            products={buckets.outOfStock}
            categoriesById={categoriesById}
          />
        </div>
      ) : null}
    </section>
  );
}

function StockAttentionSection({
  kind,
  title,
  description,
  emptyMessage,
  products,
  categoriesById,
}: {
  kind: StockSectionKind;
  title: string;
  description: string;
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
        "overflow-hidden p-0 transition-shadow duration-200",
        isLow
          ? "border-sf-warning/25 shadow-[0_1px_2px_rgba(181,71,8,0.06)]"
          : "border-sf-error/25 shadow-[0_1px_2px_rgba(217,45,32,0.06)]",
      )}
    >
      <div
        className={cx(
          "flex items-start gap-3 border-b px-4 py-4 md:px-5",
          isLow
            ? "border-sf-warning/15 bg-amber-50/70"
            : "border-sf-error/15 bg-red-50/60",
        )}
      >
        <span
          className={cx(
            "mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl",
            isLow
              ? "bg-sf-warning/10 text-sf-warning"
              : "bg-sf-error/10 text-sf-error",
          )}
          aria-hidden="true"
        >
          {isLow ? <WarningIcon /> : <PackageIcon />}
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-base font-semibold text-sf-ink md:text-lg">
              {title}
            </h3>
            <span
              className={cx(
                "inline-flex rounded-lg px-2 py-0.5 text-xs font-semibold tabular-nums",
                isLow
                  ? "bg-sf-warning/10 text-sf-warning"
                  : "bg-sf-error/10 text-sf-error",
              )}
            >
              {countLabel}
            </span>
          </div>
          <p className="mt-0.5 text-sm text-sf-muted">{description}</p>
        </div>
      </div>

      {products.length === 0 ? (
        <p className="px-4 py-6 text-sm text-sf-muted md:px-5">{emptyMessage}</p>
      ) : (
        <ul className="divide-y divide-sf-border/80">
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
          "grid gap-3 px-4 py-3.5 transition-colors duration-150 hover:bg-sf-bg/80 md:px-5",
          "md:grid-cols-[3.5rem_minmax(0,1fr)_auto_auto] md:items-center md:gap-4",
        )}
      >
        <div className="flex min-w-0 items-start gap-3 md:contents">
          <Link
            href={href}
            className="block w-14 shrink-0 overflow-hidden rounded-xl ring-1 ring-sf-border/80 transition-transform duration-150 hover:scale-[1.02] md:w-14"
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
          <div
            className={cx(
              "flex items-baseline gap-1.5 md:justify-self-end",
              "rounded-xl px-3 py-2",
              isLow ? "bg-amber-50/90" : "bg-red-50/80",
            )}
          >
            <span className="text-xs font-semibold uppercase tracking-wide text-sf-muted">
              Stock
            </span>
            <span
              className={cx(
                "text-xl font-bold tabular-nums leading-none",
                isLow ? "text-sf-warning" : "text-sf-error",
              )}
            >
              {product.stock}
            </span>
          </div>

          <div className="md:justify-self-end">
            <Link
              href={href}
              className={buttonClassName(
                "secondary",
                "w-full min-w-[8.5rem] transition-transform duration-150 hover:-translate-y-px sm:w-auto",
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
    <div className="grid gap-5" aria-hidden="true">
      {[0, 1].map((section) => (
        <Card key={section} className="grid gap-3 p-4 md:p-5">
          <div className="flex items-center gap-3">
            <Skeleton className="h-10 w-10 rounded-xl" />
            <div className="grid flex-1 gap-2">
              <Skeleton className="h-5 w-48" />
              <Skeleton className="h-4 w-64 max-w-full" />
            </div>
          </div>
          <Skeleton className="h-16 w-full rounded-xl" />
          <Skeleton className="h-16 w-full rounded-xl" />
        </Card>
      ))}
    </div>
  );
}
