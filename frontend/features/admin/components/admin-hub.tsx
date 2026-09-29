"use client";

import { useMemo, useState, type ReactNode } from "react";
import { useQueries } from "@tanstack/react-query";
import Link from "next/link";
import {
  ADMIN_SALES_ORDER_STATUSES,
  adminKeys,
  getAdminProduct,
  getAdminProductVariant,
  SALES_PERIOD_GRANULARITIES,
  type AdminProduct,
  type AdminSalesBucket,
  type SalesPeriodGranularity,
} from "@/features/admin/api";
import {
  useAdminOrdersQuery,
  useAdminProductsQuery,
  useAdminRecentBuyersQuery,
  useAdminSalesPeriodSummaryQuery,
} from "@/features/admin/hooks";
import {
  adminInventoryHref,
  adminOrderDetailHref,
  adminOrdersHref,
  adminProductDetailHref,
  aggregateRecentlySoldProducts,
  formatAdminInstant,
  isAdminRole,
  partitionAdminStockAttention,
  salesPeriodGranularityLabel,
  withDisambiguatedRecentlySoldLabels,
} from "@/features/admin/presentation";
import { orderStatusLabel } from "@/features/orders/api";
import { formatMoney, type Money } from "@/shared/money/money";
import { useSession } from "@/shared/session/session-provider";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import {
  CheckCircleIcon,
  ClipboardListIcon,
  PackageIcon,
  UserIcon,
  WarningIcon,
} from "@/shared/ui/icons";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

const RECENT_ORDERS_PAGE_SIZE = 8;
const RECENT_BUYERS_LIMIT = 8;
const SOLD_SAMPLE_SIZE = 20;
const SOLD_RANK_LIMIT = 5;

export function AdminHub() {
  return (
    <main className="px-4 py-5 md:px-7 md:py-6 lg:px-8">
      <header className="mb-5 max-w-3xl md:mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-sf-ink md:text-[1.85rem]">
          Inicio
        </h1>
        <p className="mt-1.5 text-sm leading-relaxed text-sf-muted md:text-[0.95rem]">
          Centro operativo: inventario, pedidos, ventas del período y clientes
          con compras recientes.
        </p>
      </header>

      {/* A. Inventario → B. Pedidos + vendidos → C. Ventas → D. Clientes */}
      <div className="grid gap-4 md:gap-5">
        <InventoryAttentionHero />
        <div className="grid gap-4 lg:grid-cols-2 lg:gap-5">
          <RecentOrdersCard />
          <RecentlySoldProductsCard />
        </div>
        <SalesPeriodCard />
        <RecentBuyersCard />
      </div>
    </main>
  );
}

function InventoryAttentionHero() {
  const productsQuery = useAdminProductsQuery({ status: "ACTIVE" });
  const buckets = useMemo(
    () => partitionAdminStockAttention(productsQuery.data ?? []),
    [productsQuery.data],
  );
  const total = buckets.lowStock.length + buckets.outOfStock.length;

  if (productsQuery.isPending) {
    return (
      <Card className="rounded-2xl p-5 shadow-[0_8px_24px_rgba(23,33,27,0.05)] md:p-6">
        <Skeleton className="h-6 w-56" />
        <Skeleton className="mt-4 h-24 w-full" />
      </Card>
    );
  }

  if (productsQuery.isError) {
    return (
      <Card className="rounded-2xl border-sf-border p-5 md:p-6">
        <h2 className="text-lg font-bold text-sf-ink">Atención de inventario</h2>
        <p className="mt-2 text-sm text-sf-muted">
          No se pudo cargar el inventario activo.
        </p>
      </Card>
    );
  }

  return (
    <Card
      className={cx(
        "overflow-hidden rounded-2xl border p-0 shadow-[0_8px_28px_rgba(23,33,27,0.06)]",
        total > 0
          ? "border-amber-200/80 bg-gradient-to-br from-amber-50/90 via-sf-surface to-sf-surface"
          : "border-sf-border bg-sf-surface",
      )}
    >
      <div className="flex flex-col gap-5 p-5 md:flex-row md:items-stretch md:justify-between md:p-6 lg:gap-8">
        <div className="flex min-w-0 flex-1 items-start gap-4">
          <span
            className={cx(
              "flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl shadow-sm",
              total > 0
                ? "bg-amber-100 text-amber-800"
                : "bg-emerald-100 text-emerald-700",
            )}
          >
            {total > 0 ? (
              <WarningIcon className="h-7 w-7" />
            ) : (
              <CheckCircleIcon className="h-7 w-7" />
            )}
          </span>
          <div className="min-w-0">
            <p className="text-xs font-semibold uppercase tracking-[0.08em] text-sf-muted">
              Inventario
            </p>
            <h2 className="mt-1 text-xl font-bold tracking-tight text-sf-ink md:text-2xl">
              {total > 0
                ? "Inventario requiere atención"
                : "Inventario al día"}
            </h2>
            <p className="mt-2 max-w-xl text-sm leading-relaxed text-sf-muted">
              {total > 0
                ? "Productos activos agotados (stock 0) o próximos a agotarse (1–5 unidades). Los inactivos o archivados no generan atención."
                : "No hay productos activos agotados ni próximos a agotarse."}
            </p>
            <Link
              href={adminInventoryHref()}
              className={cx(
                buttonClassName("primary"),
                "mt-4 inline-flex transition-transform duration-150 hover:-translate-y-px",
              )}
            >
              Control de inventario
            </Link>
          </div>
        </div>

        <div className="grid shrink-0 gap-3 sm:grid-cols-2 lg:w-[22rem] xl:w-[26rem]">
          <MetricTile
            href={adminInventoryHref("out")}
            label="Agotados"
            value={buckets.outOfStock.length}
            tone="danger"
            icon={PackageIcon}
          />
          <MetricTile
            href={adminInventoryHref("low")}
            label="Próximos a agotarse"
            value={buckets.lowStock.length}
            tone="warning"
            icon={WarningIcon}
          />
        </div>
      </div>
    </Card>
  );
}

function MetricTile({
  href,
  label,
  value,
  tone,
  icon: Icon,
}: {
  href: string;
  label: string;
  value: number;
  tone: "danger" | "warning";
  icon: typeof PackageIcon;
}) {
  return (
    <Link
      href={href}
      className={cx(
        "flex items-center gap-3 rounded-2xl border bg-sf-surface/90 px-4 py-4 shadow-sm",
        "transition-all duration-150 hover:-translate-y-px hover:border-sf-primary/35 hover:shadow-md",
        tone === "danger" ? "border-red-100" : "border-amber-100",
      )}
    >
      <span
        className={cx(
          "flex h-11 w-11 items-center justify-center rounded-xl",
          tone === "danger"
            ? "bg-red-50 text-red-700"
            : "bg-amber-50 text-amber-800",
        )}
      >
        <Icon className="h-5 w-5" />
      </span>
      <span>
        <span className="block text-2xl font-bold tabular-nums text-sf-ink">
          {value}
        </span>
        <span className="block text-xs font-semibold text-sf-muted">{label}</span>
      </span>
    </Link>
  );
}

function SalesPeriodCard() {
  const [granularity, setGranularity] =
    useState<SalesPeriodGranularity>("WEEK");
  const salesQuery = useAdminSalesPeriodSummaryQuery(granularity);
  const buckets = useMemo(
    () => salesQuery.data?.buckets ?? [],
    [salesQuery.data?.buckets],
  );
  const periodTotal = useMemo(
    () => buckets.reduce((sum, bucket) => sum + moneyAmount(bucket.total), 0),
    [buckets],
  );
  const periodOrders = useMemo(
    () => buckets.reduce((sum, bucket) => sum + bucket.orderCount, 0),
    [buckets],
  );

  return (
    <section aria-labelledby="admin-hub-sales-heading">
      <Card className="overflow-hidden rounded-2xl border-sf-border p-0 shadow-[0_8px_28px_rgba(23,33,27,0.05)]">
        <div className="flex flex-col gap-4 border-b border-sf-border px-5 py-4 md:flex-row md:items-start md:justify-between md:px-6 md:py-5">
          <div className="flex items-start gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-sf-primary/10 text-sf-primary">
              <ClipboardListIcon className="h-5 w-5" />
            </span>
            <div>
              <h2
                id="admin-hub-sales-heading"
                className="text-lg font-bold tracking-tight text-sf-ink"
              >
                Ventas
              </h2>
              <p className="mt-0.5 text-sm text-sf-muted">
                Totales reales del período (pedidos no cancelados).
              </p>
              {!salesQuery.isPending && !salesQuery.isError ? (
                <p className="mt-2 text-base font-bold tabular-nums text-sf-ink">
                  {formatMoney({ amount: periodTotal, currency: "COP" })}
                  <span className="ml-2 text-sm font-medium text-sf-muted">
                    · {periodOrders}{" "}
                    {periodOrders === 1 ? "pedido" : "pedidos"}
                  </span>
                </p>
              ) : null}
            </div>
          </div>
          <div
            className="inline-flex flex-wrap gap-1 rounded-xl bg-sf-bg p-1"
            role="tablist"
            aria-label="Granularidad de ventas"
          >
            {SALES_PERIOD_GRANULARITIES.map((option) => {
              const active = option === granularity;
              return (
                <button
                  key={option}
                  type="button"
                  role="tab"
                  aria-selected={active}
                  className={cx(
                    "min-h-9 rounded-lg px-3.5 text-sm font-semibold transition-all duration-150",
                    active
                      ? "bg-sf-surface text-sf-primary shadow-sm"
                      : "text-sf-muted hover:text-sf-ink",
                  )}
                  onClick={() => setGranularity(option)}
                >
                  {salesPeriodGranularityLabel(option)}
                </button>
              );
            })}
          </div>
        </div>

        <div className="px-3 py-4 md:px-5 md:py-5">
          {salesQuery.isPending ? (
            <Skeleton className="mx-2 h-72 w-[calc(100%-1rem)] md:h-80" />
          ) : salesQuery.isError ? (
            <p className="px-3 py-12 text-center text-sm text-sf-muted" role="alert">
              No se pudieron cargar las ventas del período. Comprueba la
              conexión con el API de dashboard.
            </p>
          ) : (
            <SalesChart buckets={buckets} />
          )}
        </div>
      </Card>
    </section>
  );
}

function SalesChart({ buckets }: { buckets: AdminSalesBucket[] }) {
  const amounts = buckets.map((bucket) => moneyAmount(bucket.total));
  const maxAmount = Math.max(...amounts, 0);
  const width = 960;
  const height = 320;
  const padX = 36;
  const padY = 28;
  const chartW = width - padX * 2;
  const chartH = height - padY * 2 - 32;
  const points = buckets.map((bucket, index) => {
    const x =
      buckets.length === 1
        ? padX + chartW / 2
        : padX + (index / Math.max(buckets.length - 1, 1)) * chartW;
    const ratio = maxAmount > 0 ? moneyAmount(bucket.total) / maxAmount : 0;
    const y = padY + chartH - ratio * chartH;
    return { x, y, bucket };
  });
  const linePath = points
    .map((point, index) => `${index === 0 ? "M" : "L"} ${point.x} ${point.y}`)
    .join(" ");
  const areaPath =
    points.length === 0
      ? ""
      : `${linePath} L ${points[points.length - 1]?.x ?? padX} ${padY + chartH} L ${points[0]?.x ?? padX} ${padY + chartH} Z`;

  if (buckets.length === 0) {
    return (
      <p className="px-3 py-16 text-center text-sm text-sf-muted">
        No hay ventas en este período.
      </p>
    );
  }

  const labelStep = Math.max(1, Math.ceil(buckets.length / 10));

  return (
    <div className="w-full">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="h-72 w-full md:h-80"
        role="img"
        aria-label="Gráfica de ventas del período"
      >
        <defs>
          <linearGradient id="sfSalesFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#087443" stopOpacity="0.24" />
            <stop offset="100%" stopColor="#087443" stopOpacity="0.02" />
          </linearGradient>
        </defs>
        {[0.25, 0.5, 0.75, 1].map((ratio) => {
          const y = padY + chartH * (1 - ratio);
          return (
            <line
              key={ratio}
              x1={padX}
              x2={width - padX}
              y1={y}
              y2={y}
              stroke="#e5e8e3"
              strokeDasharray="4 6"
            />
          );
        })}
        <path d={areaPath} fill="url(#sfSalesFill)" />
        <path
          d={linePath}
          fill="none"
          stroke="#087443"
          strokeWidth="2.75"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {points.map((point) => (
          <circle
            key={point.bucket.periodStart}
            cx={point.x}
            cy={point.y}
            r="4"
            fill="#ffffff"
            stroke="#087443"
            strokeWidth="2"
          >
            <title>
              {point.bucket.label}:{" "}
              {formatMoney({
                amount: moneyAmount(point.bucket.total),
                currency: "COP",
              })}{" "}
              ({point.bucket.orderCount} pedidos)
            </title>
          </circle>
        ))}
        {points.map((point, index) =>
          index % labelStep === 0 || index === points.length - 1 ? (
            <text
              key={`${point.bucket.periodStart}-label`}
              x={point.x}
              y={height - 10}
              textAnchor="middle"
              fill="#667085"
              fontSize="12"
            >
              {point.bucket.label}
            </text>
          ) : null,
        )}
      </svg>
    </div>
  );
}

function RecentOrdersCard() {
  const ordersQuery = useAdminOrdersQuery({
    page: 0,
    size: RECENT_ORDERS_PAGE_SIZE,
  });
  const orders = ordersQuery.data?.items ?? [];

  return (
    <HubTableCard
      icon={ClipboardListIcon}
      title="Últimos pedidos"
      actionHref={adminOrdersHref({})}
      actionLabel="Ver todos"
    >
      {ordersQuery.isPending ? (
        <LoadingRows />
      ) : ordersQuery.isError ? (
        <EmptyCopy>No se pudieron cargar los pedidos.</EmptyCopy>
      ) : orders.length === 0 ? (
        <EmptyCopy>Aún no hay pedidos registrados.</EmptyCopy>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[28rem] text-left text-sm">
            <thead className="bg-sf-bg/90 text-xs font-semibold uppercase tracking-wide text-sf-muted">
              <tr>
                <th className="px-5 py-3 font-semibold">Pedido</th>
                <th className="px-5 py-3 font-semibold">Estado</th>
                <th className="px-5 py-3 font-semibold">Total</th>
                <th className="px-5 py-3 font-semibold">Fecha</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sf-border/80">
              {orders.map((order) => (
                <tr
                  key={order.id}
                  className="transition-colors duration-150 hover:bg-sf-bg/60"
                >
                  <td className="px-5 py-3">
                    <Link
                      href={adminOrderDetailHref(order.id)}
                      className="font-semibold text-sf-primary transition-colors hover:underline"
                    >
                      {order.orderNumber}
                    </Link>
                  </td>
                  <td className="px-5 py-3">
                    <StatusPill status={order.status}>
                      {orderStatusLabel(order.status)}
                    </StatusPill>
                  </td>
                  <td className="px-5 py-3 font-semibold tabular-nums text-sf-ink">
                    {formatMoney(order.total)}
                  </td>
                  <td className="px-5 py-3 text-sf-muted">
                    {formatAdminInstant(order.createdAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </HubTableCard>
  );
}

function RecentlySoldProductsCard() {
  const { session } = useSession();
  const admin = isAdminRole(session?.role);
  const ordersQuery = useAdminOrdersQuery({
    page: 0,
    size: SOLD_SAMPLE_SIZE,
    status: [...ADMIN_SALES_ORDER_STATUSES],
  });
  const productsQuery = useAdminProductsQuery({});
  const aggregated = useMemo(
    () =>
      aggregateRecentlySoldProducts(
        ordersQuery.data?.items ?? [],
        SOLD_RANK_LIMIT,
      ),
    [ordersQuery.data?.items],
  );

  const listCatalogById = useMemo(() => {
    const map = new Map<string, AdminProduct>();
    for (const product of productsQuery.data ?? []) {
      map.set(product.id, product);
    }
    return map;
  }, [productsQuery.data]);

  const missingProductIds = useMemo(
    () =>
      aggregated
        .map((row) => row.productId)
        .filter((id) => !listCatalogById.has(id)),
    [aggregated, listCatalogById],
  );

  const missingProductQueries = useQueries({
    queries: missingProductIds.map((productId) => ({
      queryKey: adminKeys().product(productId),
      queryFn: () => getAdminProduct(productId),
      enabled: admin && productId.length > 0,
    })),
  });

  const catalogById = useMemo(() => {
    const map = new Map(listCatalogById);
    missingProductQueries.forEach((query) => {
      if (query.data) {
        map.set(query.data.id, query.data);
      }
    });
    return map;
  }, [listCatalogById, missingProductQueries]);

  const variantIds = useMemo(() => {
    const ids = new Set<string>();
    for (const row of aggregated) {
      const product = catalogById.get(row.productId);
      if (product?.productVariantId) {
        ids.add(product.productVariantId);
      }
    }
    return [...ids];
  }, [aggregated, catalogById]);

  const variantQueries = useQueries({
    queries: variantIds.map((variantId) => ({
      queryKey: adminKeys().productVariant(variantId),
      queryFn: () => getAdminProductVariant(variantId),
      enabled: admin && variantId.length > 0,
    })),
  });

  const variantNameById = useMemo(() => {
    const map = new Map<string, string>();
    variantQueries.forEach((query, index) => {
      const id = variantIds[index];
      if (id && query.data?.name) {
        map.set(id, query.data.name);
      }
    });
    return map;
  }, [variantIds, variantQueries]);

  const rows = useMemo(
    () =>
      withDisambiguatedRecentlySoldLabels(
        aggregated,
        catalogById,
        variantNameById,
      ),
    [aggregated, catalogById, variantNameById],
  );

  const pending =
    ordersQuery.isPending ||
    productsQuery.isPending ||
    missingProductQueries.some((query) => query.isPending) ||
    variantQueries.some((query) => query.isPending);

  return (
    <HubTableCard
      icon={PackageIcon}
      title="Productos más vendidos recientemente"
      subtitle="Según los pedidos recientes disponibles"
    >
      {pending ? (
        <LoadingRows />
      ) : ordersQuery.isError ? (
        <EmptyCopy>
          No se pudieron cargar los pedidos para este resumen.
        </EmptyCopy>
      ) : rows.length === 0 ? (
        <EmptyCopy>No hay ventas recientes para agregar productos.</EmptyCopy>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[22rem] text-left text-sm">
            <thead className="bg-sf-bg/90 text-xs font-semibold uppercase tracking-wide text-sf-muted">
              <tr>
                <th className="px-5 py-3 font-semibold">Producto</th>
                <th className="px-5 py-3 text-right font-semibold">Unidades</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sf-border/80">
              {rows.map((row, index) => (
                <tr
                  key={row.productId}
                  className="transition-colors duration-150 hover:bg-sf-bg/60"
                >
                  <td className="px-5 py-3">
                    <div className="flex items-start gap-3">
                      <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-sf-primary/10 text-xs font-bold text-sf-primary">
                        {index + 1}
                      </span>
                      <Link
                        href={adminProductDetailHref(row.productId)}
                        className="font-semibold leading-snug text-sf-primary transition-colors hover:underline"
                      >
                        {row.productName}
                      </Link>
                    </div>
                  </td>
                  <td className="px-5 py-3 text-right font-semibold tabular-nums text-sf-ink">
                    {row.quantity}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </HubTableCard>
  );
}

function RecentBuyersCard() {
  const buyersQuery = useAdminRecentBuyersQuery(RECENT_BUYERS_LIMIT);
  const buyers = buyersQuery.data?.items ?? [];

  return (
    <section aria-labelledby="admin-hub-buyers-heading">
      <HubTableCard
        icon={UserIcon}
        title="Clientes con compras recientes"
        titleId="admin-hub-buyers-heading"
        subtitle="Agrupados por cliente · últimos 30 días"
      >
        {buyersQuery.isPending ? (
          <LoadingRows />
        ) : buyersQuery.isError ? (
          <EmptyCopy>
            No se pudieron cargar los clientes con compras. Comprueba el API de
            dashboard.
          </EmptyCopy>
        ) : buyers.length === 0 ? (
          <EmptyCopy>No hay clientes con compras recientes.</EmptyCopy>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[32rem] text-left text-sm">
              <thead className="bg-sf-bg/90 text-xs font-semibold uppercase tracking-wide text-sf-muted">
                <tr>
                  <th className="px-5 py-3 font-semibold">Cliente</th>
                  <th className="px-5 py-3 font-semibold">Última compra</th>
                  <th className="px-5 py-3 font-semibold">Pedidos recientes</th>
                  <th className="px-5 py-3 font-semibold">Último total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-sf-border/80">
                {buyers.map((buyer) => (
                  <tr
                    key={buyer.customerId}
                    className="transition-colors duration-150 hover:bg-sf-bg/60"
                  >
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-sf-primary/10 text-xs font-bold text-sf-primary">
                          {buyerInitials(buyer.displayName)}
                        </span>
                        <span className="font-semibold text-sf-ink">
                          {buyer.displayName}
                        </span>
                      </div>
                    </td>
                    <td className="px-5 py-3 text-sf-muted">
                      {formatAdminInstant(buyer.lastOrderAt)}
                    </td>
                    <td className="px-5 py-3 font-semibold tabular-nums text-sf-ink">
                      {buyer.orderCount}
                    </td>
                    <td className="px-5 py-3 font-semibold tabular-nums text-sf-ink">
                      {formatMoney(buyer.lastOrderTotal)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </HubTableCard>
    </section>
  );
}

function HubTableCard({
  icon: Icon,
  title,
  titleId,
  subtitle,
  actionHref,
  actionLabel,
  children,
}: {
  icon: typeof PackageIcon;
  title: string;
  titleId?: string;
  subtitle?: string;
  actionHref?: string;
  actionLabel?: string;
  children: ReactNode;
}) {
  return (
    <Card className="overflow-hidden rounded-2xl border-sf-border p-0 shadow-[0_8px_28px_rgba(23,33,27,0.05)]">
      <div className="flex items-start justify-between gap-3 border-b border-sf-border px-5 py-4 md:px-6">
        <div className="flex items-start gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-sf-primary/10 text-sf-primary">
            <Icon className="h-4 w-4" />
          </span>
          <div>
            <h2 id={titleId} className="text-base font-bold text-sf-ink">
              {title}
            </h2>
            {subtitle ? (
              <p className="mt-0.5 text-xs text-sf-muted">{subtitle}</p>
            ) : null}
          </div>
        </div>
        {actionHref && actionLabel ? (
          <Link
            href={actionHref}
            className="shrink-0 rounded-lg px-2 py-1 text-sm font-semibold text-sf-primary transition-colors hover:bg-sf-primary/10"
          >
            {actionLabel}
          </Link>
        ) : null}
      </div>
      {children}
    </Card>
  );
}

function LoadingRows() {
  return (
    <div className="space-y-3 p-5">
      <Skeleton className="h-10 w-full" />
      <Skeleton className="h-10 w-full" />
      <Skeleton className="h-10 w-full" />
    </div>
  );
}

function EmptyCopy({ children }: { children: ReactNode }) {
  return <p className="px-5 py-8 text-sm text-sf-muted">{children}</p>;
}

function StatusPill({
  children,
  status,
}: {
  children: string;
  status: string;
}) {
  const tone =
    status === "DELIVERED"
      ? "bg-emerald-50 text-emerald-800 ring-emerald-100"
      : status === "CANCELLED"
        ? "bg-red-50 text-red-700 ring-red-100"
        : status === "CONFIRMED" ||
            status === "PREPARING" ||
            status === "DELIVERY"
          ? "bg-amber-50 text-amber-900 ring-amber-100"
          : "bg-sf-bg text-sf-ink ring-sf-border";
  return (
    <span
      className={cx(
        "inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1",
        tone,
      )}
    >
      {children}
    </span>
  );
}

function buyerInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return "?";
  }
  if (parts.length === 1) {
    return parts[0]!.slice(0, 2).toUpperCase();
  }
  return `${parts[0]!.charAt(0)}${parts[1]!.charAt(0)}`.toUpperCase();
}

function moneyAmount(money: Money | { amount: number | string }): number {
  const raw = money.amount;
  return typeof raw === "number" ? raw : Number(raw);
}
