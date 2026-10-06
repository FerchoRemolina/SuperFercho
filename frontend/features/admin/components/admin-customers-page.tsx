"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import type { FormEvent } from "react";
import type {
  AdminCustomerRecordListItem,
  AdminCustomerRecordStatusFilter,
  ListAdminCustomersQuery,
} from "@/features/admin/api";
import { useAdminCustomersQuery } from "@/features/admin/hooks";
import {
  ADMIN_CUSTOMERS_PERSPECTIVES,
  ADMIN_CUSTOMERS_SORT_OPTIONS,
  ADMIN_CUSTOMERS_STATUS_FILTERS,
  activeAdminCustomersPerspective,
  adminCustomerDetailHref,
  adminCustomerDisplayName,
  adminCustomerDocumentLabel,
  adminCustomerPageRangeLabel,
  adminCustomerRecordStatusLabel,
  adminCustomerRecordStatusToneClass,
  adminCustomersHref,
  adminCustomersListQueryFromSearchParams,
  adminCustomersPerspectiveParams,
  adminCustomersSortFromSelectValue,
  adminCustomersSortSelectValue,
  canGoToNextAdminCustomersPage,
  canGoToPreviousAdminCustomersPage,
  type AdminCustomersPerspective,
} from "@/features/admin/customers-presentation";
import { formatAdminInstant } from "@/features/admin/presentation";
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

export function AdminCustomersPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const listQuery = adminCustomersListQueryFromSearchParams({
    search: searchParams.get("search"),
    status: searchParams.get("status"),
    sortBy: searchParams.get("sortBy"),
    sortDir: searchParams.get("sortDir"),
    hasPurchases: searchParams.get("hasPurchases"),
    page: searchParams.get("page"),
  });
  const activePerspective = activeAdminCustomersPerspective(
    {
      sortBy: listQuery.sortBy ?? "CREATED_AT",
      sortDir: listQuery.sortDir ?? "DESC",
      hasPurchases: listQuery.hasPurchases,
    },
    searchParams.get("perspective"),
  );
  const customersQuery = useAdminCustomersQuery(listQuery);
  const canPrevious = canGoToPreviousAdminCustomersPage(listQuery.page);
  const canNext = customersQuery.data
    ? canGoToNextAdminCustomersPage(
        customersQuery.data.page,
        customersQuery.data.size,
        customersQuery.data.totalElements,
      )
    : false;
  const hasActiveFilters =
    Boolean(listQuery.search) ||
    (listQuery.status != null && listQuery.status !== "ALL") ||
    listQuery.hasPurchases !== undefined ||
    activePerspective !== "all";

  function replaceList(next: {
    search?: string;
    status?: AdminCustomerRecordStatusFilter;
    sortBy?: ListAdminCustomersQuery["sortBy"];
    sortDir?: ListAdminCustomersQuery["sortDir"];
    hasPurchases?: boolean | null;
    page?: number;
    perspective?: AdminCustomersPerspective | null;
  }) {
    const hasPurchases =
      next.hasPurchases === null
        ? undefined
        : (next.hasPurchases ?? listQuery.hasPurchases);
    const sortBy = next.sortBy ?? listQuery.sortBy;
    const sortDir = next.sortDir ?? listQuery.sortDir;
    const perspective =
      next.perspective === null
        ? undefined
        : (next.perspective ??
          (activePerspective === "recent" ? "recent" : undefined));

    router.replace(
      adminCustomersHref({
        search: next.search ?? listQuery.search,
        status: next.status ?? listQuery.status,
        sortBy,
        sortDir,
        hasPurchases,
        page: next.page ?? listQuery.page,
        perspective: perspective === "recent" ? "recent" : undefined,
      }),
    );
  }

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const submitted = String(
      new FormData(event.currentTarget).get("search") ?? "",
    ).trim();
    replaceList({ search: submitted, page: 0 });
  }

  function onPerspective(perspective: AdminCustomersPerspective) {
    const preset = adminCustomersPerspectiveParams(perspective);
    replaceList({
      sortBy: preset.sortBy,
      sortDir: preset.sortDir,
      hasPurchases: preset.hasPurchases ?? null,
      page: 0,
      perspective,
    });
  }

  return (
    <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
      <p className="text-xs font-semibold uppercase tracking-[0.08em] text-sf-muted">
        Clientes
      </p>
      <h1 className="mt-1 text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
        Gestión de clientes
      </h1>
      <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
        Consulta, filtra y administra el registro comercial de tus clientes
        desde un solo listado.
      </p>

      <div
        className="mt-6 inline-flex flex-wrap gap-1 rounded-xl bg-sf-bg p-1"
        role="group"
        aria-label="Perspectivas del listado"
      >
        {ADMIN_CUSTOMERS_PERSPECTIVES.map((perspective) => {
          const selected = activePerspective === perspective.id;
          return (
            <button
              key={perspective.id}
              type="button"
              aria-pressed={selected}
              onClick={() => onPerspective(perspective.id)}
              className={cx(
                "inline-flex min-h-8 items-center whitespace-nowrap rounded-lg px-3 text-sm font-semibold transition-colors",
                "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
                selected
                  ? "bg-sf-surface text-sf-primary shadow-sm"
                  : "text-sf-muted hover:text-sf-ink",
              )}
            >
              {perspective.label}
            </button>
          );
        })}
      </div>

      <Card className="mt-5 p-4 md:p-5">
        <form
          className="flex flex-col gap-3 sm:flex-row sm:items-end"
          role="search"
          onSubmit={onSearch}
        >
          <div className="min-w-0 flex-1">
            <label
              htmlFor="admin-customers-search"
              className="mb-1 block text-xs font-medium text-sf-muted"
            >
              Buscar
            </label>
            <div className="relative">
              <SearchIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-sf-muted" />
              <input
                id="admin-customers-search"
                type="search"
                name="search"
                defaultValue={listQuery.search ?? ""}
                key={listQuery.search ?? ""}
                placeholder="Buscar por nombre, documento, email o teléfono"
                className="min-h-10 w-full rounded-lg border border-sf-border bg-sf-surface py-2 pl-10 pr-3 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary"
              />
            </div>
          </div>
          <Button type="submit" className="min-h-10 px-4 text-sm">
            Buscar
          </Button>
        </form>

        <div className="mt-4 flex flex-wrap items-end gap-3">
          <div className="w-44">
            <SelectField
              id="admin-customers-status"
              label="Estado"
              labelClassName="text-xs font-medium text-sf-muted"
              className="w-full min-w-0"
              value={listQuery.status ?? "ALL"}
              onChange={(event) =>
                replaceList({
                  status: event.target
                    .value as AdminCustomerRecordStatusFilter,
                  page: 0,
                })
              }
            >
              {ADMIN_CUSTOMERS_STATUS_FILTERS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </SelectField>
          </div>
          <div className="w-52">
            <SelectField
              id="admin-customers-sort"
              label="Ordenar por"
              labelClassName="text-xs font-medium text-sf-muted"
              className="w-full min-w-0"
              value={adminCustomersSortSelectValue(
                listQuery.sortBy ?? "CREATED_AT",
                listQuery.sortDir ?? "DESC",
              )}
              onChange={(event) => {
                const next = adminCustomersSortFromSelectValue(
                  event.target.value,
                );
                replaceList({
                  sortBy: next.sortBy,
                  sortDir: next.sortDir,
                  page: 0,
                  perspective: null,
                });
              }}
            >
              {ADMIN_CUSTOMERS_SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </SelectField>
          </div>
          {hasActiveFilters ? (
            <button
              type="button"
              onClick={() =>
                router.replace(
                  adminCustomersHref({ perspective: "all" }),
                )
              }
              className="min-h-10 text-sm font-semibold text-sf-primary hover:underline"
            >
              Limpiar filtros
            </button>
          ) : null}
        </div>
      </Card>

      {customersQuery.isFetching && !customersQuery.isPending ? (
        <p className="mt-3 text-xs text-sf-muted" aria-live="polite">
          Actualizando listado…
        </p>
      ) : null}

      {customersQuery.isPending ? (
        <div className="mt-5 grid gap-2" aria-hidden="true">
          {[0, 1, 2, 3, 4, 5].map((row) => (
            <div
              key={row}
              className="flex items-center gap-4 rounded-xl border border-sf-border bg-sf-surface px-4 py-3"
            >
              <div className="min-w-0 flex-1 space-y-2">
                <Skeleton className="h-4 w-48" />
                <Skeleton className="h-3 w-32" />
              </div>
              <Skeleton className="hidden h-4 w-40 md:block" />
              <Skeleton className="hidden h-4 w-24 lg:block" />
              <Skeleton className="hidden h-4 w-28 lg:block" />
              <Skeleton className="h-5 w-16" />
            </div>
          ))}
        </div>
      ) : null}

      {customersQuery.isError ? (
        <div className="mt-5 grid gap-3">
          <Alert tone="error" title="No se pudo cargar el listado de clientes">
            {isApiError(customersQuery.error)
              ? messageForApiProblem(customersQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => void customersQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      ) : null}

      {customersQuery.isSuccess && customersQuery.data.items.length === 0 ? (
        <div className="mt-5">
          <EmptyState
            title="No hay clientes para mostrar"
            description={
              hasActiveFilters
                ? "Prueba con otra búsqueda, perspectiva o quita los filtros."
                : "Cuando existan clientes registrados, aparecerán en este listado."
            }
            action={
              hasActiveFilters ? (
                <Link
                  href="/admin/customers"
                  className={buttonClassName("secondary")}
                >
                  Ver todos
                </Link>
              ) : undefined
            }
          />
        </div>
      ) : null}

      {customersQuery.isSuccess && customersQuery.data.items.length > 0 ? (
        <>
          <ul className="mt-5 grid gap-2 lg:hidden">
            {customersQuery.data.items.map((customer) => (
              <li key={customer.id}>
                <CustomerMobileCard customer={customer} />
              </li>
            ))}
          </ul>

          <Card className="mt-5 hidden overflow-hidden p-0 lg:block">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[68rem] text-left text-sm">
                <thead>
                  <tr className="border-b border-sf-border bg-sf-bg/60 text-xs font-medium text-sf-muted">
                    <th className="px-4 py-3 font-medium">Cliente</th>
                    <th className="px-4 py-3 font-medium">Contacto</th>
                    <th className="px-4 py-3 font-medium">Actividad</th>
                    <th className="px-4 py-3 text-right font-medium">Gasto</th>
                    <th className="px-4 py-3 font-medium">Estado</th>
                    <th className="px-4 py-3 text-right font-medium">
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-sf-border/80">
                  {customersQuery.data.items.map((customer) => (
                    <tr
                      key={customer.id}
                      className="transition-colors duration-150 hover:bg-sf-bg/60"
                    >
                      <td className="px-4 py-3 align-top">
                        <p className="text-[15px] font-semibold text-sf-ink">
                          {adminCustomerDisplayName(customer)}
                        </p>
                        <p className="mt-0.5 text-xs tabular-nums text-sf-muted">
                          {adminCustomerDocumentLabel(
                            customer.documentType,
                            customer.documentNumber,
                          )}
                        </p>
                      </td>
                      <td className="px-4 py-3 align-top">
                        <p className="truncate text-sm text-sf-ink">
                          {customer.email ?? "—"}
                        </p>
                        <p className="mt-0.5 text-xs text-sf-muted">
                          {customer.phone ?? "Sin teléfono"}
                        </p>
                      </td>
                      <td className="px-4 py-3 align-top">
                        <p className="text-sm font-semibold tabular-nums text-sf-ink">
                          {customer.orderCount}{" "}
                          {customer.orderCount === 1 ? "pedido" : "pedidos"}
                        </p>
                        <p className="mt-0.5 text-xs text-sf-muted">
                          {customer.lastOrderAt
                            ? `Última: ${formatAdminInstant(customer.lastOrderAt)}`
                            : "Sin compras"}
                        </p>
                      </td>
                      <td className="px-4 py-3 align-top text-right font-bold tabular-nums text-sf-ink">
                        {formatMoney(customer.totalSpent)}
                      </td>
                      <td className="px-4 py-3 align-top">
                        <span
                          className={cx(
                            "inline-flex items-center rounded-lg px-2 py-0.5 text-[11px] font-semibold",
                            adminCustomerRecordStatusToneClass(customer.status),
                          )}
                        >
                          {adminCustomerRecordStatusLabel(customer.status)}
                        </span>
                      </td>
                      <td className="px-4 py-3 align-top text-right">
                        <Link
                          href={adminCustomerDetailHref(customer.id)}
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
            <div className="flex items-center justify-between gap-3 border-t border-sf-border px-4 py-2.5">
              <p className="text-xs text-sf-muted">
                {adminCustomerPageRangeLabel(
                  customersQuery.data.page,
                  customersQuery.data.size,
                  customersQuery.data.totalElements,
                  "cliente",
                  "clientes",
                )}
              </p>
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  aria-label="Página anterior"
                  disabled={!canPrevious}
                  onClick={() => replaceList({ page: listQuery.page - 1 })}
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
                  onClick={() => replaceList({ page: listQuery.page + 1 })}
                  className={cx(
                    "inline-flex h-8 min-w-8 items-center justify-center rounded-lg border border-sf-border px-2 text-sm font-semibold text-sf-ink transition-colors",
                    !canNext
                      ? "cursor-not-allowed opacity-50"
                      : "hover:bg-sf-bg",
                  )}
                >
                  ›
                </button>
              </div>
            </div>
          </Card>

          <div className="mt-4 flex items-center justify-between gap-3 lg:hidden">
            <p className="text-xs text-sf-muted">
              {adminCustomerPageRangeLabel(
                customersQuery.data.page,
                customersQuery.data.size,
                customersQuery.data.totalElements,
                "cliente",
                "clientes",
              )}
            </p>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                aria-label="Página anterior"
                disabled={!canPrevious}
                onClick={() => replaceList({ page: listQuery.page - 1 })}
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
                onClick={() => replaceList({ page: listQuery.page + 1 })}
                className={cx(
                  "inline-flex h-8 min-w-8 items-center justify-center rounded-lg border border-sf-border px-2 text-sm font-semibold text-sf-ink transition-colors",
                  !canNext
                    ? "cursor-not-allowed opacity-50"
                    : "hover:bg-sf-bg",
                )}
              >
                ›
              </button>
            </div>
          </div>
        </>
      ) : null}
    </main>
  );
}

function CustomerMobileCard({
  customer,
}: {
  customer: AdminCustomerRecordListItem;
}) {
  return (
    <Card className="p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate font-semibold text-sf-ink">
            {adminCustomerDisplayName(customer)}
          </p>
          <p className="mt-0.5 text-xs tabular-nums text-sf-muted">
            {adminCustomerDocumentLabel(
              customer.documentType,
              customer.documentNumber,
            )}
          </p>
        </div>
        <span
          className={cx(
            "inline-flex shrink-0 items-center rounded-lg px-2 py-0.5 text-[11px] font-semibold",
            adminCustomerRecordStatusToneClass(customer.status),
          )}
        >
          {adminCustomerRecordStatusLabel(customer.status)}
        </span>
      </div>
      <dl className="mt-3 grid gap-2 text-sm">
        <div>
          <dt className="text-xs text-sf-muted">Contacto</dt>
          <dd className="mt-0.5 text-sf-ink">{customer.email ?? "—"}</dd>
          <dd className="text-xs text-sf-muted">
            {customer.phone ?? "Sin teléfono"}
          </dd>
        </div>
        <div className="flex items-end justify-between gap-3">
          <div>
            <dt className="text-xs text-sf-muted">Actividad</dt>
            <dd className="mt-0.5 text-sm font-semibold tabular-nums text-sf-ink">
              {customer.orderCount}{" "}
              {customer.orderCount === 1 ? "pedido" : "pedidos"}
            </dd>
            <dd className="text-xs text-sf-muted">
              {customer.lastOrderAt
                ? `Última: ${formatAdminInstant(customer.lastOrderAt)}`
                : "Sin compras"}
            </dd>
          </div>
          <div className="text-right">
            <dt className="text-xs text-sf-muted">Gasto</dt>
            <dd className="mt-0.5 font-bold tabular-nums text-sf-ink">
              {formatMoney(customer.totalSpent)}
            </dd>
          </div>
        </div>
      </dl>
      <div className="mt-3 flex justify-end">
        <Link
          href={adminCustomerDetailHref(customer.id)}
          className={cx(
            buttonClassName("secondary"),
            "min-h-8 rounded-lg px-3 text-sm",
          )}
        >
          Ver detalle
        </Link>
      </div>
    </Card>
  );
}
