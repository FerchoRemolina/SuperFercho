"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ADMIN_ORDER_STATUSES } from "@/features/admin/api";
import { useAdminOrdersQuery } from "@/features/admin/hooks";
import {
  adminOrderDetailHref,
  adminOrdersHref,
  adminOrdersListQueryFromSearchParams,
  adminOrdersPageCount,
  canGoToNextAdminOrdersPage,
  canGoToPreviousAdminOrdersPage,
  formatAdminInstant,
} from "@/features/admin/presentation";
import { orderStatusLabel, type OrderStatus } from "@/features/orders/api";
import { OrderStatusBadge } from "@/features/orders/components/order-status-badge";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { SelectField } from "@/shared/ui/select-field";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminOrdersPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const listQuery = adminOrdersListQueryFromSearchParams({
    page: searchParams.get("page"),
    status: searchParams.get("status"),
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

  function replaceFilters(next: {
    page?: number;
    status?: OrderStatus | "";
  }) {
    router.replace(
      adminOrdersHref({
        page: next.page ?? listQuery.page,
        status:
          next.status === undefined ? (listQuery.status ?? "") : next.status,
      }),
    );
  }

  return (
    <Container as="main" className="py-10 md:py-16">
      <div>
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Pedidos
        </h1>
        <p className="mt-2 max-w-2xl text-base text-sf-muted">
          Consulta los pedidos de todos los clientes. El detalle se abre en una
          pantalla aparte.
        </p>
      </div>

      <div className="mt-8 max-w-sm">
        <SelectField
          id="admin-order-status"
          label="Estado"
          value={listQuery.status ?? ""}
          onChange={(event) =>
            replaceFilters({
              page: 0,
              status: (event.target.value || "") as OrderStatus | "",
            })
          }
        >
          <option value="">Todos</option>
          {ADMIN_ORDER_STATUSES.map((status) => (
            <option key={status} value={status}>
              {orderStatusLabel(status)}
            </option>
          ))}
        </SelectField>
      </div>

      {ordersQuery.isPending ? (
        <div className="mt-8 grid gap-3" aria-hidden="true">
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-28 w-full" />
        </div>
      ) : null}

      {ordersQuery.isError ? (
        <div className="mt-8 grid gap-4">
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
        <div className="mt-8">
          <EmptyState
            title="No hay pedidos para mostrar"
            description={
              listQuery.status
                ? "Prueba con otro estado o quita el filtro."
                : "Cuando existan pedidos, aparecerán en este listado."
            }
            action={
              listQuery.status ? (
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
          <ul className="mt-8 grid gap-4 md:hidden">
            {ordersQuery.data.items.map((order) => (
              <li key={order.id}>
                <Card className="grid gap-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm text-sf-muted">Número de pedido</p>
                      <p className="text-lg font-semibold text-sf-ink">
                        {order.orderNumber}
                      </p>
                    </div>
                    <OrderStatusBadge status={order.status} />
                  </div>
                  <dl className="grid gap-2 text-sm">
                    <div>
                      <dt className="text-sf-muted">Fecha</dt>
                      <dd className="mt-1 font-semibold text-sf-ink">
                        {formatAdminInstant(order.createdAt)}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-sf-muted">Cliente</dt>
                      <dd className="mt-1 break-all font-semibold text-sf-ink">
                        {order.customerId}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-sf-muted">Total</dt>
                      <dd className="mt-1 font-bold text-sf-ink">
                        {formatMoney(order.total)}
                      </dd>
                    </div>
                  </dl>
                  <Link
                    href={adminOrderDetailHref(order.id)}
                    className={buttonClassName("secondary")}
                  >
                    Ver detalle
                  </Link>
                </Card>
              </li>
            ))}
          </ul>

          <div className="mt-8 hidden overflow-x-auto md:block">
            <table className="w-full min-w-[52rem] border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-sf-border text-sf-muted">
                  <th className="px-3 py-3 font-semibold">Número</th>
                  <th className="px-3 py-3 font-semibold">Fecha</th>
                  <th className="px-3 py-3 font-semibold">Cliente</th>
                  <th className="px-3 py-3 font-semibold">Estado</th>
                  <th className="px-3 py-3 font-semibold">Total</th>
                  <th className="px-3 py-3 font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {ordersQuery.data.items.map((order) => (
                  <tr
                    key={order.id}
                    className="border-b border-sf-border align-top"
                  >
                    <td className="px-3 py-4 font-semibold text-sf-ink">
                      {order.orderNumber}
                    </td>
                    <td className="px-3 py-4 text-sf-muted">
                      {formatAdminInstant(order.createdAt)}
                    </td>
                    <td className="px-3 py-4 break-all text-sf-muted">
                      {order.customerId}
                    </td>
                    <td className="px-3 py-4">
                      <OrderStatusBadge status={order.status} />
                    </td>
                    <td className="px-3 py-4 font-semibold text-sf-ink">
                      {formatMoney(order.total)}
                    </td>
                    <td className="px-3 py-4">
                      <Link
                        href={adminOrderDetailHref(order.id)}
                        className={buttonClassName("secondary")}
                      >
                        Ver detalle
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-sf-muted">
              Página {ordersQuery.data.page + 1}
              {pageCount > 0 ? ` de ${pageCount}` : ""}
              {" · "}
              {ordersQuery.data.totalElements} pedido
              {ordersQuery.data.totalElements === 1 ? "" : "s"}
            </p>
            <div className="flex gap-2">
              <Button
                type="button"
                variant="secondary"
                disabled={!canPrevious}
                onClick={() =>
                  replaceFilters({ page: Math.max(0, listQuery.page - 1) })
                }
              >
                Anterior
              </Button>
              <Button
                type="button"
                variant="secondary"
                disabled={!canNext}
                onClick={() => replaceFilters({ page: listQuery.page + 1 })}
              >
                Siguiente
              </Button>
            </div>
          </div>
        </>
      ) : null}
    </Container>
  );
}
