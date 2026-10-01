"use client";

import { useMemo, type ReactNode } from "react";
import { useQueries } from "@tanstack/react-query";
import Link from "next/link";
import {
  ADMIN_SALES_ORDER_STATUSES,
  adminKeys,
  getAdminProduct,
  getAdminProductVariant,
  type AdminProduct,
  type OrderStatus,
} from "@/features/admin/api";
import {
  useAdminOrdersQuery,
  useAdminProductsQuery,
  useAdminRecentBuyersQuery,
  useAdminSalesPeriodSummaryQuery,
} from "@/features/admin/hooks";
import { AnalyticsCard } from "@/features/admin/components/admin-analytics-card";
import {
  adminInventoryHref,
  adminOrderDetailHref,
  adminOrdersHref,
  adminProductDetailHref,
  aggregateRecentlySoldProducts,
  formatAdminInstant,
  isAdminRole,
  partitionAdminStockAttention,
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

const RECENT_ORDERS_PAGE_SIZE = 5;
const RECENT_BUYERS_LIMIT = 5;
const SOLD_SAMPLE_SIZE = 20;
const SOLD_RANK_LIMIT = 5;

const IN_PROCESS_ORDER_STATUSES: readonly OrderStatus[] = [
  "CONFIRMED",
  "PREPARING",
  "DELIVERY",
] as const;

export function AdminHub() {
  return (
    <main className="px-4 py-4 md:px-8 md:py-5">
      <header className="mb-4 max-w-3xl">
        <h1 className="text-2xl font-bold tracking-tight text-sf-ink md:text-[1.75rem]">
          Inicio
        </h1>
        <p className="mt-1 text-sm leading-relaxed text-sf-muted md:text-[0.95rem]">
          Centro operativo de SuperFercho: inventario, pedidos, ventas del
          período y clientes con compras recientes.
        </p>
      </header>

      {/* B. Atención → C. Resumen → D. Reciente → E. Analítica → F. Clientes */}
      <div className="grid gap-3.5">
        <InventoryAttentionHero />
        <BusinessSummaryStrip />
        <div className="grid gap-3.5 lg:grid-cols-2">
          <RecentOrdersCard />
          <RecentlySoldProductsCard />
        </div>
        <AnalyticsCard />
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
      <Card className="rounded-xl p-5 shadow-[0_1px_2px_rgba(16,24,40,0.05)]">
        <Skeleton className="h-6 w-56" />
        <Skeleton className="mt-4 h-24 w-full" />
      </Card>
    );
  }

  if (productsQuery.isError) {
    return (
      <Card className="rounded-xl border-sf-border p-5">
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
        "overflow-hidden rounded-xl border p-0 shadow-[0_1px_2px_rgba(16,24,40,0.05)]",
        total > 0 ? "border-amber-200/80 bg-sf-surface" : "border-sf-border bg-sf-surface",
      )}
    >
      <div className="flex flex-col gap-4 p-4 md:flex-row md:items-center md:justify-between lg:gap-6">
        <div className="flex min-w-0 flex-1 items-start gap-3.5">
          <span
            className={cx(
              "flex h-11 w-11 shrink-0 items-center justify-center rounded-xl",
              total > 0
                ? "bg-amber-100 text-amber-800"
                : "bg-emerald-100 text-emerald-700",
            )}
          >
            {total > 0 ? (
              <WarningIcon className="h-5 w-5" />
            ) : (
              <CheckCircleIcon className="h-5 w-5" />
            )}
          </span>
          <div className="min-w-0">
            <h2 className="text-lg font-bold tracking-tight text-sf-ink">
              {total > 0
                ? "Inventario requiere atención"
                : "Inventario al día"}
            </h2>
            <p className="mt-1 max-w-xl text-sm leading-relaxed text-sf-muted">
              {total > 0
                ? "Hay productos que necesitan reposición."
                : "No hay productos que requieran reposición."}
            </p>
            {total > 0 ? (
              <Link
                href={adminInventoryHref()}
                className={cx(buttonClassName("primary"), "mt-3 inline-flex")}
              >
                Control de inventario
              </Link>
            ) : null}
          </div>
        </div>

        {total > 0 ? (
          <div className="grid shrink-0 gap-3 sm:grid-cols-2 lg:w-[20rem]">
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
        ) : null}
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
        "flex items-center gap-3 rounded-lg border bg-sf-surface px-3.5 py-2.5 shadow-[0_1px_2px_rgba(16,24,40,0.04)]",
        "transition-colors duration-150 hover:border-sf-primary/35",
        tone === "danger" ? "border-red-100" : "border-amber-100",
      )}
    >
      <span
        className={cx(
          "flex h-9 w-9 items-center justify-center rounded-lg",
          tone === "danger"
            ? "bg-red-50 text-red-700"
            : "bg-amber-50 text-amber-800",
        )}
      >
        <Icon className="h-4.5 w-4.5" />
      </span>
      <span>
        <span className="block text-lg font-bold tabular-nums text-sf-ink">
          {value}
        </span>
        <span className="block text-xs font-semibold text-sf-muted">{label}</span>
      </span>
    </Link>
  );
}

/**
 * C. Resumen general: estado real de la operación con datos del API.
 * - "En proceso" = CONFIRMED + PREPARING + DELIVERY (activos, sin entregar ni
 *   cancelar). Los conteos de pedidos son totales acumulados (no existe hoy
 *   un endpoint de conteos por período); ventas es la cifra de la semana.
 * Pendientes futuros (sin endpoint hoy): conteos por período, KPI de pagos.
 */
function BusinessSummaryStrip() {
  const inProcessQuery = useAdminOrdersQuery({
    page: 0,
    size: 1,
    status: [...IN_PROCESS_ORDER_STATUSES],
  });
  const deliveredQuery = useAdminOrdersQuery({
    page: 0,
    size: 1,
    status: ["DELIVERED"],
  });
  const cancelledQuery = useAdminOrdersQuery({
    page: 0,
    size: 1,
    status: ["CANCELLED"],
  });
  const weekSalesQuery = useAdminSalesPeriodSummaryQuery("WEEK");

  const weekTotal = useMemo(
    () =>
      (weekSalesQuery.data?.buckets ?? []).reduce(
        (sum, bucket) => sum + moneyAmount(bucket.total),
        0,
      ),
    [weekSalesQuery.data?.buckets],
  );

  function orderCount(
    query: typeof inProcessQuery | typeof deliveredQuery | typeof cancelledQuery,
  ): string | null {
    if (query.isPending) {
      return null;
    }
    return query.isError ? "—" : String(query.data?.totalElements ?? 0);
  }

  const metrics = [
    {
      label: "Pedidos en proceso",
      value: orderCount(inProcessQuery),
      caption: "Requieren gestión",
    },
    {
      label: "Pedidos entregados",
      value: orderCount(deliveredQuery),
      caption: "Completados",
    },
    {
      label: "Pedidos cancelados",
      value: orderCount(cancelledQuery),
      caption: "Cancelados",
    },
    {
      label: "Ventas",
      value: weekSalesQuery.isPending
        ? null
        : weekSalesQuery.isError
          ? "—"
          : formatMoney({ amount: weekTotal, currency: "COP" }),
      caption: "Esta semana",
    },
  ];

  return (
    <Card className="overflow-hidden rounded-xl border-sf-border p-0 shadow-[0_1px_2px_rgba(16,24,40,0.05)]">
      <div className="grid gap-px bg-sf-border sm:grid-cols-2 lg:grid-cols-4">
        {metrics.map((metric) => (
          <SummaryMetric key={metric.label} {...metric} />
        ))}
      </div>
    </Card>
  );
}

function SummaryMetric({
  label,
  value,
  caption,
}: {
  label: string;
  value: string | null;
  caption: string;
}) {
  return (
    <div className="bg-sf-surface px-4 py-3">
      <div className="text-xl font-bold tracking-tight tabular-nums text-sf-ink">
        {value ?? <Skeleton className="h-6 w-24" />}
      </div>
      <p className="mt-0.5 text-sm font-semibold text-sf-ink">{label}</p>
      <p className="mt-0.5 text-xs text-sf-muted">{caption}</p>
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
          <table className="w-full min-w-[24rem] text-left text-sm">
            <thead className="bg-sf-bg/80 text-[13px] text-sf-muted">
              <tr>
                <th className="px-4 py-2 font-medium">Pedido</th>
                <th className="px-4 py-2 font-medium">Estado</th>
                <th className="px-4 py-2 font-medium">Total</th>
                <th className="px-4 py-2 font-medium">Fecha</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sf-border/80">
              {orders.map((order) => (
                <tr
                  key={order.id}
                  className="transition-colors duration-150 hover:bg-sf-bg/60"
                >
                  <td className="px-4 py-2">
                    <Link
                      href={adminOrderDetailHref(order.id)}
                      className="font-semibold text-sf-primary transition-colors hover:underline"
                    >
                      {order.orderNumber}
                    </Link>
                  </td>
                  <td className="px-4 py-2">
                    <StatusPill status={order.status}>
                      {orderStatusLabel(order.status)}
                    </StatusPill>
                  </td>
                  <td className="px-4 py-2 font-semibold tabular-nums text-sf-ink">
                    {formatMoney(order.total)}
                  </td>
                  <td className="px-4 py-2 text-sf-muted">
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
          <table className="w-full min-w-[20rem] text-left text-sm">
            <thead className="bg-sf-bg/80 text-[13px] text-sf-muted">
              <tr>
                <th className="px-4 py-2 font-medium">Producto</th>
                <th className="px-4 py-2 text-right font-medium">Unidades</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sf-border/80">
              {rows.map((row, index) => (
                <tr
                  key={row.productId}
                  className="transition-colors duration-150 hover:bg-sf-bg/60"
                >
                  <td className="px-4 py-2">
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
                  <td className="px-4 py-2 text-right font-semibold tabular-nums text-sf-ink">
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
            <table className="w-full min-w-[26rem] text-left text-sm">
              <thead className="bg-sf-bg/80 text-[13px] text-sf-muted">
                <tr>
                  <th className="px-4 py-2 font-medium">Cliente</th>
                  <th className="px-4 py-2 font-medium">Última compra</th>
                  <th className="px-4 py-2 font-medium">Pedidos recientes</th>
                  <th className="px-4 py-2 text-right font-medium">Último total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-sf-border/80">
                {buyers.map((buyer) => (
                  <tr
                    key={buyer.customerId}
                    className="transition-colors duration-150 hover:bg-sf-bg/60"
                  >
                    <td className="px-4 py-2">
                      <div className="flex items-center gap-2.5">
                        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-sf-primary/10 text-xs font-bold text-sf-primary">
                          {buyerInitials(buyer.displayName)}
                        </span>
                        <span className="truncate font-semibold text-sf-ink">
                          {buyer.displayName}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-2 text-sf-muted">
                      {formatAdminInstant(buyer.lastOrderAt)}
                    </td>
                    <td className="px-4 py-2 font-semibold tabular-nums text-sf-ink">
                      {buyer.orderCount}
                    </td>
                    <td className="px-4 py-2 text-right font-semibold tabular-nums text-sf-ink">
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
    <Card className="overflow-hidden rounded-xl border-sf-border p-0 shadow-[0_1px_2px_rgba(16,24,40,0.05)]">
      <div className="flex items-center justify-between gap-3 border-b border-sf-border px-4 py-3 md:px-5">
        <div className="flex items-center gap-3">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-sf-primary/10 text-sf-primary">
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
            className="inline-flex shrink-0 items-center justify-center rounded-lg border border-sf-border bg-sf-surface px-3 py-1.5 text-sm font-semibold text-sf-primary transition-colors hover:bg-sf-bg"
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
    <div className="space-y-3 p-4">
      <Skeleton className="h-9 w-full" />
      <Skeleton className="h-9 w-full" />
      <Skeleton className="h-9 w-full" />
    </div>
  );
}

function EmptyCopy({ children }: { children: ReactNode }) {
  return <p className="px-5 py-7 text-sm text-sf-muted">{children}</p>;
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
