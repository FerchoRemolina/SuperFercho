"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import type { FormEvent } from "react";
import {
  ADMIN_ORDER_STATUSES,
  type AdminOrderListItem,
  type ListAdminOrdersQuery,
} from "@/features/admin/api";
import { useAdminOrdersQuery } from "@/features/admin/hooks";
import {
  adminOrderCustomerPrimaryLabel,
  adminOrderCustomerSecondaryLabel,
  adminOrderDetailHref,
  adminOrdersFilterSelectValue,
  adminOrdersHref,
  adminOrdersListQueryFromSearchParams,
  adminOrdersPageCount,
  adminOrdersStatusFromSelectValue,
  canGoToNextAdminOrdersPage,
  canGoToPreviousAdminOrdersPage,
  formatAdminInstant,
} from "@/features/admin/presentation";
import { orderStatusLabel } from "@/features/orders/api";
import { OrderStatusBadge } from "@/features/orders/components/order-status-badge";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { EmptyState } from "@/shared/ui/empty-state";
import { SearchIcon } from "@/shared/ui/icons";
import { SelectField } from "@/shared/ui/select-field";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

function customerPrimaryLabel(order: AdminOrderListItem): string {
  return order.customer
    ? adminOrderCustomerPrimaryLabel(order)
    : "Cliente no disponible";
}

function customerSecondaryLabel(order: AdminOrderListItem): string | null {
  if (!order.customer) {
    return null;
  }
  return adminOrderCustomerSecondaryLabel(order) ?? "Sin documento";
}

function ordersPageRangeLabel(
  page: number,
  size: number,
  totalElements: number,
): string {
  if (totalElements <= 0) {
    return "0 pedidos";
  }
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return `Mostrando ${from} a ${to} de ${totalElements} ${
    totalElements === 1 ? "pedido" : "pedidos"
  }`;
}

export function AdminOrdersPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const listQuery = adminOrdersListQueryFromSearchParams({
    page: searchParams.get("page"),
    status: searchParams.get("status"),
    orderNumber: searchParams.get("orderNumber"),
    from: searchParams.get("from"),
    to: searchParams.get("to"),
  });
  const ordersQuery = useAdminOrdersQuery(listQuery);
  const pageCount = ordersQuery.data
    ? adminOrdersPageCount(ordersQuery.data.totalElements, ordersQuery.data.size)
    : 0;
  const canPrevious = canGoToPreviousAdminOrdersPage(listQuery.page);
  const canNext = ordersQuery.data
    ? canGoToNextAdminOrdersPage(
        ordersQuery.data.page,
        ordersQuery.data.size,
        ordersQuery.data.totalElements,
      )
    : false;
  const hasActiveFilters =
    Boolean(listQuery.status) ||
    Boolean(listQuery.orderNumber) ||
    Boolean(listQuery.from) ||
    Boolean(listQuery.to);

  function replaceFilters(next: {
    page?: number;
    status?: ListAdminOrdersQuery["status"] | "";
    orderNumber?: string | null;
    from?: string | null;
    to?: string | null;
  }) {
    router.replace(
      adminOrdersHref({
        page: next.page ?? listQuery.page,
        status:
          next.status === undefined ? (listQuery.status ?? "") : next.status,
        orderNumber:
          next.orderNumber === undefined
            ? listQuery.orderNumber
            : (next.orderNumber ?? undefined),
        from: next.from === undefined ? listQuery.from : (next.from ?? undefined),
        to: next.to === undefined ? listQuery.to : (next.to ?? undefined),
      }),
    );
  }

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const submitted = String(
      new FormData(event.currentTarget).get("orderNumber") ?? "",
    ).trim();
    replaceFilters({ orderNumber: submitted || null, page: 0 });
  }

  return (
    <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
      <p className="text-xs font-semibold uppercase tracking-[0.08em] text-sf-muted">
        Ventas
      </p>
      <h1 className="mt-1 text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
        Pedidos
      </h1>
      <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
        Consulta los pedidos de todos los clientes. Ventas agrupa Confirmado,
        En preparación, En camino y Entregado.
      </p>

      <Card className="mt-5 p-4 md:p-5">
        <form
          className="flex flex-col gap-3 sm:flex-row sm:items-end"
          role="search"
          onSubmit={onSearch}
        >
          <div className="min-w-0 flex-1">
            <label
              htmlFor="admin-orders-search"
              className="mb-1 block text-xs font-medium text-sf-muted"
            >
              Buscar por número de pedido
            </label>
            <div className="relative">
              <SearchIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-sf-muted" />
              <input
                id="admin-orders-search"
                type="search"
                name="orderNumber"
                defaultValue={listQuery.orderNumber ?? ""}
                key={listQuery.orderNumber ?? ""}
                placeholder="Ej. ORD-1A2B3C4D5E6F"
                className="min-h-10 w-full rounded-lg border border-sf-border bg-sf-surface py-2 pl-10 pr-3 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary"
              />
            </div>
          </div>
          <Button type="submit" className="min-h-10 px-4 text-sm">
            Buscar
          </Button>
        </form>

        <div className="mt-4 flex flex-wrap items-end gap-3">
          <div className="w-40">
            <label
              htmlFor="admin-orders-from"
              className="mb-1 block text-xs font-medium text-sf-muted"
            >
              Desde
            </label>
            <input
              id="admin-orders-from"
              type="date"
              value={listQuery.from ?? ""}
              onChange={(event) =>
                replaceFilters({ from: event.target.value || null, page: 0 })
              }
              className="min-h-10 w-full rounded-lg border border-sf-border bg-sf-surface px-3 py-2 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary"
            />
          </div>
          <div className="w-40">
            <label
              htmlFor="admin-orders-to"
              className="mb-1 block text-xs font-medium text-sf-muted"
            >
              Hasta
            </label>
            <input
              id="admin-orders-to"
              type="date"
              value={listQuery.to ?? ""}
              onChange={(event) =>
                replaceFilters({ to: event.target.value || null, page: 0 })
              }
              className="min-h-10 w-full rounded-lg border border-sf-border bg-sf-surface px-3 py-2 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary"
            />
          </div>
          <div className="w-44">
            <SelectField
              id="admin-order-status"
              label="Estado"
              labelClassName="text-xs font-medium text-sf-muted"
              className="w-full min-w-0"
              value={adminOrdersFilterSelectValue(listQuery.status)}
              onChange={(event) =>
                replaceFilters({
                  page: 0,
                  status: adminOrdersStatusFromSelectValue(event.target.value),
                })
              }
            >
              <option value="">Todos</option>
              <option value="sales">Ventas</option>
              {ADMIN_ORDER_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {orderStatusLabel(status)}
                </option>
              ))}
            </SelectField>
          </div>
          {hasActiveFilters ? (
            <button
              type="button"
              onClick={() => router.replace("/admin/orders")}
              className="min-h-10 text-sm font-semibold text-sf-primary hover:underline"
            >
              Limpiar filtros
            </button>
          ) : null}
        </div>
      </Card>

      {ordersQuery.isFetching && !ordersQuery.isPending ? (
        <p className="mt-3 text-xs text-sf-muted" aria-live="polite">
          Actualizando listado…
        </p>
      ) : null}

      {ordersQuery.isPending ? (
        <div className="mt-5 grid gap-2" aria-hidden="true">
          {[0, 1, 2, 3, 4, 5].map((row) => (
            <div
              key={row}
              className="flex items-center gap-4 rounded-xl border border-sf-border bg-sf-surface px-4 py-3"
            >
              <div className="min-w-0 flex-1 space-y-2">
                <Skeleton className="h-4 w-40" />
                <Skeleton className="h-3 w-28" />
              </div>
              <Skeleton className="hidden h-4 w-36 md:block" />
              <Skeleton className="hidden h-4 w-24 sm:block" />
              <Skeleton className="h-5 w-20" />
            </div>
          ))}
        </div>
      ) : null}

      {ordersQuery.isError ? (
        <div className="mt-5 grid gap-3">
          <Alert tone="error" title="No se pudieron cargar los pedidos">
            {isApiError(ordersQuery.error)
              ? messageForApiProblem(ordersQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => void ordersQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      ) : null}

      {ordersQuery.isSuccess && ordersQuery.data.items.length === 0 ? (
        <div className="mt-5">
          <EmptyState
            title="No hay pedidos para mostrar"
            description={
              hasActiveFilters
                ? "Prueba con otro número, rango de fechas o quita los filtros."
                : "Cuando existan pedidos, aparecerán en este listado."
            }
            action={
              hasActiveFilters ? (
                <Link
                  href="/admin/orders"
                  className={buttonClassName("secondary")}
                >
                  Ver todos
                </Link>
              ) : undefined
            }
          />
        </div>
      ) : null}

      {ordersQuery.isSuccess && ordersQuery.data.items.length > 0 ? (
        <>
          <ul className="mt-5 grid gap-2 lg:hidden">
            {ordersQuery.data.items.map((order) => (
              <li key={order.id}>
                <Card className="p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate text-[15px] font-semibold tabular-nums text-sf-ink">
                        {order.orderNumber}
                      </p>
                      <p className="mt-0.5 text-xs text-sf-muted">
                        {formatAdminInstant(order.createdAt)}
                      </p>
                    </div>
                    <OrderStatusBadge status={order.status} />
                  </div>
                  <dl className="mt-3 grid gap-2 text-sm">
                    <div>
                      <dt className="text-xs text-sf-muted">Cliente</dt>
                      <dd className="mt-0.5 font-semibold text-sf-ink">
                        {customerPrimaryLabel(order)}
                      </dd>
                      <dd className="text-xs tabular-nums text-sf-muted">
                        {customerSecondaryLabel(order)}
                      </dd>
                    </div>
                    <div className="flex items-end justify-between gap-3">
                      <div>
                        <dt className="text-xs text-sf-muted">Fecha</dt>
                        <dd className="mt-0.5 text-sm text-sf-ink">
                          {formatAdminInstant(order.createdAt)}
                        </dd>
                      </div>
                      <div className="text-right">
                        <dt className="text-xs text-sf-muted">Total</dt>
                        <dd className="mt-0.5 font-bold tabular-nums text-sf-ink">
                          {formatMoney(order.total)}
                        </dd>
                      </div>
                    </div>
                  </dl>
                  <div className="mt-3 flex justify-end">
                    <Link
                      href={adminOrderDetailHref(order.id)}
                      className={cx(
                        buttonClassName("secondary"),
                        "min-h-8 rounded-lg px-3 text-sm",
                      )}
                    >
                      Ver detalle
                    </Link>
                  </div>
                </Card>
              </li>
            ))}
          </ul>

          <OrdersPager
            listQuery={listQuery}
            pageCount={pageCount}
            totalElements={ordersQuery.data.totalElements}
            canPrevious={canPrevious}
            canNext={canNext}
            onPrevious={() => replaceFilters({ page: listQuery.page - 1 })}
            onNext={() => replaceFilters({ page: listQuery.page + 1 })}
            className="mt-4 lg:hidden"
          />

          <Card className="mt-5 hidden overflow-hidden p-0 lg:block">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[64rem] text-left text-sm">
                <thead>
                  <tr className="border-b border-sf-border bg-sf-bg/60 text-xs font-medium text-sf-muted">
                    <th className="px-4 py-3 font-medium">Número</th>
                    <th className="px-4 py-3 font-medium">Fecha</th>
                    <th className="px-4 py-3 font-medium">Cliente</th>
                    <th className="px-4 py-3 font-medium">Estado</th>
                    <th className="px-4 py-3 text-right font-medium">Total</th>
                    <th className="px-4 py-3 text-right font-medium">
                      <span className="sr-only">Acción</span>
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-sf-border/80">
                  {ordersQuery.data.items.map((order) => (
                    <tr
                      key={order.id}
                      className="transition-colors duration-150 hover:bg-sf-bg/60"
                    >
                      <td className="px-4 py-3 align-top">
                        <p className="font-semibold tabular-nums text-sf-ink">
                          {order.orderNumber}
                        </p>
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 align-top text-sf-muted">
                        {formatAdminInstant(order.createdAt)}
                      </td>
                      <td className="px-4 py-3 align-top">
                        <p className="text-[15px] font-semibold text-sf-ink">
                          {customerPrimaryLabel(order)}
                        </p>
                        <p className="mt-0.5 text-xs tabular-nums text-sf-muted">
                          {customerSecondaryLabel(order)}
                        </p>
                      </td>
                      <td className="px-4 py-3 align-top">
                        <OrderStatusBadge status={order.status} />
                      </td>
                      <td className="px-4 py-3 text-right align-top font-semibold tabular-nums text-sf-ink">
                        {formatMoney(order.total)}
                      </td>
                      <td className="px-4 py-3 text-right align-top">
                        <Link
                          href={adminOrderDetailHref(order.id)}
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
            <OrdersPager
              listQuery={listQuery}
              pageCount={pageCount}
              totalElements={ordersQuery.data.totalElements}
              canPrevious={canPrevious}
              canNext={canNext}
              onPrevious={() => replaceFilters({ page: listQuery.page - 1 })}
              onNext={() => replaceFilters({ page: listQuery.page + 1 })}
              className="border-t border-sf-border"
            />
          </Card>
        </>
      ) : null}
    </main>
  );
}

function OrdersPager({
  listQuery,
  pageCount,
  totalElements,
  canPrevious,
  canNext,
  onPrevious,
  onNext,
  className,
}: {
  listQuery: ListAdminOrdersQuery;
  pageCount: number;
  totalElements: number;
  canPrevious: boolean;
  canNext: boolean;
  onPrevious: () => void;
  onNext: () => void;
  className?: string;
}) {
  return (
    <div
      className={cx(
        "flex items-center justify-between gap-3 px-4 py-2.5",
        className,
      )}
    >
      <p className="text-xs text-sf-muted">
        {ordersPageRangeLabel(listQuery.page, listQuery.size, totalElements)}
        {pageCount > 1 ? ` · Página ${listQuery.page + 1} de ${pageCount}` : ""}
      </p>
      <div className="flex items-center gap-1.5">
        <button
          type="button"
          aria-label="Página anterior"
          disabled={!canPrevious}
          onClick={onPrevious}
          className={cx(
            "inline-flex h-8 min-w-8 items-center justify-center rounded-lg border border-sf-border px-2 text-sm font-semibold text-sf-ink transition-colors",
            !canPrevious
              ? "cursor-not-allowed opacity-50"
              : "hover:bg-sf-bg",
          )}
        >
          ‹
        </button>
        <span className="inline-flex h-8 min-w-8 items-center justify-center rounded-lg bg-sf-primary/10 px-2 text-sm font-semibold text-sf-primary">
          {listQuery.page + 1}
        </span>
        <button
          type="button"
          aria-label="Página siguiente"
          disabled={!canNext}
          onClick={onNext}
          className={cx(
            "inline-flex h-8 min-w-8 items-center justify-center rounded-lg border border-sf-border px-2 text-sm font-semibold text-sf-ink transition-colors",
            !canNext ? "cursor-not-allowed opacity-50" : "hover:bg-sf-bg",
          )}
        >
          ›
        </button>
      </div>
    </div>
  );
}
