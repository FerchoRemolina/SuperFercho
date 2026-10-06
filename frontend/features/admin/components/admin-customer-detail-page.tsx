"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { Fragment, useState } from "react";
import {
  useActivateAdminCustomerAccountMutation,
  useAdminCustomerOrdersQuery,
  useAdminCustomerPaymentsQuery,
  useAdminCustomerRecordQuery,
  useDeactivateAdminCustomerAccountMutation,
} from "@/features/admin/hooks";
import {
  adminCustomerAccountsSummary,
  adminCustomerAccountsSummaryLabel,
  adminCustomerAccountStatus,
  adminCustomerAccountStatusLabel,
  adminCustomerAccountToneClass,
  adminCustomerDocumentTypeLabel,
  adminCustomerOrderStatusToneClass,
  adminCustomerPageRangeLabel,
  adminCustomerPaymentStatusLabel,
  adminCustomerPaymentStatusToneClass,
} from "@/features/admin/customers-presentation";
import {
  orderStatusLabel,
  paymentMethodLabel,
} from "@/features/orders/api";
import type {
  OrderStatus,
  PaymentMethod,
  PaymentStatus,
} from "@/features/orders/api";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatAdminInstant } from "@/features/admin/presentation";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { EmptyState } from "@/shared/ui/empty-state";
import { CartIcon, InfoIcon, UserIcon } from "@/shared/ui/icons";
import { SelectField } from "@/shared/ui/select-field";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

const CUSTOMER_HISTORY_PAGE_SIZE = 10;

type CustomerTab = "accounts" | "orders" | "payments" | "info";

const CUSTOMER_TABS: readonly { id: CustomerTab; label: string }[] = [
  { id: "accounts", label: "Cuentas" },
  { id: "orders", label: "Historial de compras" },
  { id: "payments", label: "Historial de pagos" },
  { id: "info", label: "Información comercial" },
];

function shortRef(id: string): string {
  return `#${id.slice(0, 8)}`;
}

function moneyValue(money: { amount: number | string }): number {
  return typeof money.amount === "number" ? money.amount : Number(money.amount);
}

export function AdminCustomerDetailPageContent() {
  const params = useParams<{ customerId: string }>();
  const customerRecordId =
    typeof params.customerId === "string" ? params.customerId : "";

  const [accountFilter, setAccountFilter] = useState<
    "ALL" | "ACTIVE" | "INACTIVE" | "DELETED"
  >("ALL");
  const [activeTab, setActiveTab] = useState<CustomerTab>("accounts");
  const [ordersPage, setOrdersPage] = useState(0);
  const [paymentsPage, setPaymentsPage] = useState(0);
  const [confirmingAccount, setConfirmingAccount] = useState<{
    userId: string;
    action: "activate" | "deactivate";
  } | null>(null);

  const recordQuery = useAdminCustomerRecordQuery(
    customerRecordId,
    accountFilter,
    customerRecordId.length > 0,
  );
  const record = recordQuery.data;

  const ordersQuery = useAdminCustomerOrdersQuery(
    record?.id ?? "",
    ordersPage,
    CUSTOMER_HISTORY_PAGE_SIZE,
    record != null,
  );
  const paymentsQuery = useAdminCustomerPaymentsQuery(
    record?.id ?? "",
    paymentsPage,
    CUSTOMER_HISTORY_PAGE_SIZE,
    record != null,
  );

  const activate = useActivateAdminCustomerAccountMutation();
  const deactivate = useDeactivateAdminCustomerAccountMutation();
  const pendingUserId = activate.isPending
    ? (activate.variables?.userId ?? null)
    : deactivate.isPending
      ? (deactivate.variables?.userId ?? null)
      : null;
  const accountError = activate.error ?? deactivate.error;
  const accountErrorMessage = accountError
    ? isApiError(accountError)
      ? messageForApiProblem(accountError.problem)
      : "No se pudo completar la solicitud."
    : null;

  function confirmAccountAction() {
    if (!confirmingAccount || pendingUserId != null || !record) {
      return;
    }
    const mutation =
      confirmingAccount.action === "activate" ? activate : deactivate;
    mutation.mutate(
      { customerRecordId: record.id, userId: confirmingAccount.userId },
      { onSuccess: () => setConfirmingAccount(null) },
    );
  }

  const accountEmails = useMemoAccountEmails(record?.accounts);
  const orderNumbers = orderNumbersFrom(ordersQuery.data?.items);
  const ordersById = ordersByIdFrom(ordersQuery.data?.items);

  const notFound =
    recordQuery.isError &&
    isApiError(recordQuery.error) &&
    recordQuery.error.problem.status === 404;

  return (
    <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
      <Link
        href="/admin/customers"
        className="text-sm font-semibold text-sf-primary hover:underline"
      >
        ← Volver al listado
      </Link>
      <p className="mt-4 text-xs font-semibold uppercase tracking-[0.08em] text-sf-muted">
        Clientes
      </p>
      <h1 className="mt-1 text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
        Detalle del cliente
      </h1>
      <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
        Consulta y administra la información comercial y las cuentas
        asociadas.
      </p>

      {recordQuery.isPending ? <CustomerLoadingSkeleton /> : null}

      {recordQuery.isError && !notFound ? (
        <div className="mt-4">
          <Alert tone="error" title="No se pudo cargar el cliente">
            {isApiError(recordQuery.error)
              ? messageForApiProblem(recordQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
        </div>
      ) : null}

      {notFound ? (
        <div className="mt-4">
          <EmptyState
            title="No encontramos este cliente."
            description="Puede haber sido eliminado o el enlace ya no es válido."
            action={
              <Link
                href="/admin/customers"
                className={buttonClassName("secondary")}
              >
                Volver al listado
              </Link>
            }
          />
        </div>
      ) : null}

      {record != null ? (
        <>
          <CustomerRecordSummary
            record={record}
            ordersTotal={ordersQuery.data?.totalElements}
            paymentsTotal={paymentsQuery.data?.totalElements}
          />

          <div
            role="tablist"
            aria-label="Información del cliente"
            className="mt-6 flex gap-6 overflow-x-auto border-b border-sf-border"
          >
            {CUSTOMER_TABS.map((tab) => (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={activeTab === tab.id}
                aria-controls={`customer-tab-${tab.id}`}
                onClick={() => setActiveTab(tab.id)}
                className={cx(
                  "whitespace-nowrap border-b-2 pb-2.5 pt-1 text-sm font-semibold transition-colors",
                  activeTab === tab.id
                    ? "border-sf-primary text-sf-primary"
                    : "border-transparent text-sf-muted hover:text-sf-ink",
                )}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <div className="mt-4">
            {activeTab === "accounts" ? (
              <div
                id="customer-tab-accounts"
                role="tabpanel"
                aria-label="Cuentas"
              >
                <AccountsTab
                  accounts={record.accounts}
                  filter={accountFilter}
                  confirming={confirmingAccount}
                  pendingUserId={pendingUserId}
                  errorMessage={accountErrorMessage}
                  onConfirmAction={(userId, action) =>
                    setConfirmingAccount({ userId, action })
                  }
                  onFilterChange={(filter) => {
                    setAccountFilter(filter);
                    setConfirmingAccount(null);
                  }}
                  onCancelConfirm={() => setConfirmingAccount(null)}
                  onConfirm={confirmAccountAction}
                />
              </div>
            ) : null}

            {activeTab === "orders" ? (
              <div
                id="customer-tab-orders"
                role="tabpanel"
                aria-label="Historial de compras"
              >
                <OrdersTab
                  record={record}
                  query={ordersQuery}
                  page={ordersPage}
                  onPageChange={setOrdersPage}
                />
              </div>
            ) : null}

            {activeTab === "payments" ? (
              <div
                id="customer-tab-payments"
                role="tabpanel"
                aria-label="Historial de pagos"
              >
                <PaymentsTab
                  query={paymentsQuery}
                  page={paymentsPage}
                  onPageChange={setPaymentsPage}
                  ordersById={ordersById}
                  accountEmails={accountEmails}
                  orderNumbers={orderNumbers}
                />
              </div>
            ) : null}

            {activeTab === "info" ? (
              <div
                id="customer-tab-info"
                role="tabpanel"
                aria-label="Información comercial"
              >
                <CommercialInfoTab record={record} />
              </div>
            ) : null}
          </div>
        </>
      ) : null}
    </main>
  );
}

function useMemoAccountEmails(
  accounts: { id: string; email: string }[] | undefined,
): Map<string, string> {
  const map = new Map<string, string>();
  for (const account of accounts ?? []) {
    map.set(account.id, account.email);
  }
  return map;
}

function orderNumbersFrom(
  orders: { id: string; orderNumber: string }[] | undefined,
): Map<string, string> {
  const map = new Map<string, string>();
  for (const order of orders ?? []) {
    map.set(order.id, order.orderNumber);
  }
  return map;
}

function ordersByIdFrom(
  orders: { id: string; customerId: string }[] | undefined,
): Map<string, string> {
  const map = new Map<string, string>();
  for (const order of orders ?? []) {
    map.set(order.id, order.customerId);
  }
  return map;
}

function CustomerRecordSummary({
  record,
  ordersTotal,
  paymentsTotal,
}: {
  record: {
    documentType: string;
    documentNumber: string;
    billingFirstName: string;
    billingLastName: string;
    createdAt: string;
    accounts: { status: "ACTIVE" | "INACTIVE"; deletedAt: string | null }[];
  };
  ordersTotal: number | undefined;
  paymentsTotal: number | undefined;
}) {
  const summary = adminCustomerAccountsSummary(record.accounts);
  const summaryLabel = adminCustomerAccountsSummaryLabel(summary);

  return (
    <Card className="mt-4 p-5">
      <div className="flex flex-col gap-6 lg:flex-row lg:items-center">
        <div className="flex min-w-0 flex-1 items-center gap-4">
          <span className="flex size-16 shrink-0 items-center justify-center rounded-full bg-sf-primary/10 text-sf-primary">
            <UserIcon className="h-8 w-8" />
          </span>
          <div className="min-w-0">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-sf-muted">
              Cliente
            </p>
            <p className="mt-0.5 truncate text-2xl font-bold text-sf-ink">
              {record.billingFirstName} {record.billingLastName}
            </p>
            <p className="mt-0.5 text-sm text-sf-muted">
              {adminCustomerDocumentTypeLabel(record.documentType)}{" "}
              {record.documentNumber}
            </p>
            <p className="mt-1 text-xs text-sf-muted">
              Cliente desde {formatAdminInstant(record.createdAt)}
            </p>
            <p className="mt-1 text-xs font-medium text-sf-ink">
              {summaryLabel}
            </p>
          </div>
        </div>

        <div
          aria-hidden="true"
          className="hidden w-px self-stretch bg-sf-border lg:block"
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3 lg:w-[34rem] lg:shrink-0">
          <SummaryMetric
            icon={<UserIcon className="h-5 w-5" />}
            value={String(summary.total)}
            label="Cuentas asociadas"
          />
          <SummaryMetric
            icon={<CartIcon className="h-5 w-5" />}
            value={ordersTotal != null ? String(ordersTotal) : "…"}
            label="Pedidos realizados"
          />
          <SummaryMetric
            icon={<InfoIcon className="h-5 w-5" />}
            value={paymentsTotal != null ? String(paymentsTotal) : "…"}
            label="Pagos registrados"
          />
        </div>
      </div>
    </Card>
  );
}

function SummaryMetric({
  icon,
  value,
  label,
}: {
  icon: React.ReactNode;
  value: string;
  label: string;
}) {
  return (
    <div className="flex items-center gap-3">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-sf-primary/10 text-sf-primary">
        {icon}
      </span>
      <div className="min-w-0">
        <p className="text-xl font-bold tabular-nums text-sf-ink">{value}</p>
        <p className="truncate text-xs text-sf-muted">{label}</p>
      </div>
    </div>
  );
}

function CustomerLoadingSkeleton() {
  return (
    <>
      <Card className="mt-4 p-5">
        <div className="flex items-center gap-4" aria-hidden="true">
          <Skeleton className="size-16 rounded-full" />
          <div className="min-w-0 flex-1 space-y-2">
            <Skeleton className="h-3 w-28" />
            <Skeleton className="h-6 w-64" />
            <Skeleton className="h-4 w-40" />
            <Skeleton className="h-3 w-32" />
          </div>
          <div className="hidden gap-6 lg:flex">
            {[0, 1, 2].map((metric) => (
              <Skeleton key={metric} className="h-12 w-24" />
            ))}
          </div>
        </div>
      </Card>
      <div className="mt-6" aria-hidden="true">
        <Skeleton className="h-9 w-full max-w-md" />
        <Skeleton className="mt-6 h-40 w-full" />
      </div>
    </>
  );
}

function AccountsTab({
  accounts,
  filter,
  confirming,
  pendingUserId,
  errorMessage,
  onConfirmAction,
  onFilterChange,
  onCancelConfirm,
  onConfirm,
}: {
  accounts: {
    id: string;
    email: string;
    phone: string | null;
    status: "ACTIVE" | "INACTIVE";
    deletedAt: string | null;
    createdAt: string;
  }[];
  filter: "ALL" | "ACTIVE" | "INACTIVE" | "DELETED";
  confirming: { userId: string; action: "activate" | "deactivate" } | null;
  pendingUserId: string | null;
  errorMessage: string | null;
  onConfirmAction: (userId: string, action: "activate" | "deactivate") => void;
  onFilterChange: (filter: "ALL" | "ACTIVE" | "INACTIVE" | "DELETED") => void;
  onCancelConfirm: () => void;
  onConfirm: () => void;
}) {
  return (
    <Card className="overflow-hidden p-0">
      <div className="flex flex-col gap-1 border-b border-sf-border px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-base font-semibold text-sf-ink">
            Cuentas asociadas
          </h3>
          <p className="text-xs text-sf-muted">
            Cuentas de usuario vinculadas a este cliente.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-sf-muted">Mostrar</span>
          <div className="w-44">
            <SelectField
              id="admin-customer-accounts-filter"
              label="Mostrar cuentas"
              labelClassName="sr-only"
              className="w-full min-w-0"
              value={filter}
              onChange={(event) =>
                onFilterChange(
                  event.target.value as
                    | "ALL"
                    | "ACTIVE"
                    | "INACTIVE"
                    | "DELETED",
                )
              }
            >
              <option value="ALL">Todas las cuentas</option>
              <option value="ACTIVE">Activas</option>
              <option value="INACTIVE">Inactivas</option>
              <option value="DELETED">Eliminadas</option>
            </SelectField>
          </div>
        </div>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[44rem] text-left text-sm">
          <thead>
            <tr className="border-b border-sf-border bg-sf-bg/60 text-xs font-medium text-sf-muted">
              <th className="px-4 py-2 font-medium">Email</th>
              <th className="px-4 py-2 font-medium">Teléfono</th>
              <th className="px-4 py-2 font-medium">Estado</th>
              <th className="px-4 py-2 font-medium">Fecha de creación</th>
              <th className="px-4 py-2 text-right font-medium">Acción</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-sf-border/80">
            {accounts.map((account) => {
              const status = adminCustomerAccountStatus(account);
              const action =
                status === "ACTIVE"
                  ? "deactivate"
                  : status === "INACTIVE"
                    ? "activate"
                    : null;
              const confirmingThis =
                confirming?.userId === account.id &&
                (pendingUserId != null || errorMessage != null ||
                  confirming.action === action);
              return (
                <Fragment key={account.id}>
                  <tr
                    className={cx(
                      "transition-colors duration-150",
                      confirmingThis
                        ? "bg-sf-bg/80"
                        : "hover:bg-sf-bg/60",
                    )}
                  >
                    <td className="px-4 py-2.5 text-sf-ink">{account.email}</td>
                    <td className="px-4 py-2.5 text-sf-muted">
                      {account.phone ?? "—"}
                    </td>
                    <td className="px-4 py-2.5">
                      <span
                        className={cx(
                          "inline-flex items-center rounded-lg px-2 py-0.5 text-[11px] font-semibold",
                          adminCustomerAccountToneClass(status),
                        )}
                      >
                        {adminCustomerAccountStatusLabel(status)}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-sf-muted">
                      {formatAdminInstant(account.createdAt)}
                    </td>
                    <td className="px-4 py-2.5 text-right">
                      {action && !confirmingThis ? (
                        <button
                          type="button"
                          disabled={pendingUserId != null}
                          onClick={() => onConfirmAction(account.id, action)}
                          className={cx(
                            "inline-flex min-h-8 items-center justify-center rounded-lg border px-3 text-sm font-semibold transition-colors",
                            "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
                            action === "deactivate"
                              ? "border-sf-error/40 bg-sf-surface text-sf-error hover:bg-red-50"
                              : "border-sf-primary/40 bg-sf-surface text-sf-primary hover:bg-sf-primary/10",
                            pendingUserId != null &&
                              "cursor-not-allowed opacity-60",
                          )}
                        >
                          {action === "deactivate" ? "Desactivar" : "Activar"}
                        </button>
                      ) : status === "DELETED" ? (
                        <span className="text-sf-muted">—</span>
                      ) : pendingUserId === account.id ? (
                        <span className="text-xs text-sf-muted">Guardando…</span>
                      ) : null}
                    </td>
                  </tr>
                  {confirmingThis ? (
                    <tr key={`${account.id}-confirm`}>
                      <td colSpan={5} className="bg-sf-bg/70 px-4 py-3">
                        {errorMessage ? (
                          <p className="mb-2 text-sm text-sf-error" role="alert">
                            {errorMessage}
                          </p>
                        ) : null}
                        <p className="text-sm font-semibold text-sf-ink">
                          {confirming?.action === "deactivate"
                            ? "¿Desactivar esta cuenta de usuario?"
                            : "¿Activar esta cuenta de usuario?"}
                        </p>
                        <div className="mt-2 flex items-center gap-2">
                          <button
                            type="button"
                            disabled={pendingUserId != null}
                            onClick={onConfirm}
                            className={cx(
                              "inline-flex min-h-8 items-center justify-center rounded-lg px-3.5 text-sm font-semibold transition-colors",
                              confirming?.action === "deactivate"
                                ? "bg-sf-error text-white hover:opacity-90"
                                : "bg-sf-primary text-white hover:bg-sf-primary-hover",
                              "disabled:cursor-not-allowed disabled:opacity-60",
                            )}
                          >
                            {pendingUserId != null
                              ? "Guardando…"
                              : confirming?.action === "deactivate"
                                ? "Sí, desactivar"
                                : "Sí, activar"}
                          </button>
                          <button
                            type="button"
                            disabled={pendingUserId != null}
                            onClick={onCancelConfirm}
                            className="inline-flex min-h-8 items-center justify-center rounded-lg border border-sf-border bg-sf-surface px-3.5 text-sm font-semibold text-sf-ink transition-colors hover:bg-sf-bg disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            Cancelar
                          </button>
                        </div>
                      </td>
                     </tr>
                   ) : null}
                 </Fragment>
               );
             })}
          </tbody>
        </table>
      </div>
      <p className="border-t border-sf-border px-4 py-2.5 text-xs text-sf-muted">
        {accounts.length} {accounts.length === 1 ? "cuenta" : "cuentas"}
      </p>
    </Card>
  );
}

function OrdersTab({
  record,
  query,
  page,
  onPageChange,
}: {
  record: { accounts: { id: string; email: string }[] };
  query: {
    isPending: boolean;
    isError: boolean;
    data?:
      | {
          items: {
            id: string;
            orderNumber: string;
            customerId: string;
            status: OrderStatus;
            subtotal: { amount: number | string };
            total: { amount: number | string };
            paymentId: string | null;
            createdAt: string;
          }[];
          totalElements: number;
        }
      | undefined;
    refetch: () => void;
  };
  page: number;
  onPageChange: (page: number) => void;
}) {
  if (query.isPending) {
    return (
      <Card className="p-0">
        <div className="space-y-2 p-4" aria-hidden="true">
          {[0, 1, 2, 3].map((row) => (
            <Skeleton key={row} className="h-9 w-full" />
          ))}
        </div>
      </Card>
    );
  }
  if (query.isError) {
    return (
      <Card className="p-4">
        <Alert tone="error" title="No se pudo cargar el historial de compras">
          <button
            type="button"
            onClick={() => query.refetch()}
            className="font-semibold underline underline-offset-2"
          >
            Reintentar
          </button>
        </Alert>
      </Card>
    );
  }
  const items = query.data?.items ?? [];
  if (items.length === 0) {
    return (
      <Card className="p-0">
        <EmptyState
          title="Este cliente no tiene pedidos registrados."
          description="Cuando el cliente realice compras, aparecerán aquí."
        />
      </Card>
    );
  }
  const emails = new Map(record.accounts.map((a) => [a.id, a.email]));
  return (
    <Card className="overflow-hidden p-0">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[52rem] text-left text-sm">
          <thead>
            <tr className="border-b border-sf-border bg-sf-bg/60 text-xs font-medium text-sf-muted">
              <th className="px-4 py-2 font-medium">Pedido</th>
              <th className="px-4 py-2 font-medium">Cuenta</th>
              <th className="px-4 py-2 font-medium">Estado</th>
              <th className="px-4 py-2 text-right font-medium">Subtotal</th>
              <th className="px-4 py-2 text-right font-medium">Total</th>
              <th className="px-4 py-2 font-medium">Pago</th>
              <th className="px-4 py-2 font-medium">Fecha</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-sf-border/80">
            {items.map((order) => (
              <tr
                key={order.id}
                className="transition-colors duration-150 hover:bg-sf-bg/60"
              >
                <td className="px-4 py-2.5 font-semibold text-sf-ink">
                  {order.orderNumber}
                </td>
                <td className="px-4 py-2.5 text-sf-muted">
                  {emails.get(order.customerId) ?? "—"}
                </td>
                <td className="px-4 py-2.5">
                  <span
                    className={cx(
                      "inline-flex items-center rounded-lg px-2 py-0.5 text-[11px] font-semibold",
                      adminCustomerOrderStatusToneClass(order.status),
                    )}
                  >
                    {orderStatusLabel(order.status)}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-right tabular-nums text-sf-muted">
                  {formatMoney({ amount: moneyValue(order.subtotal), currency: "COP" })}
                </td>
                <td className="px-4 py-2.5 text-right font-semibold tabular-nums text-sf-ink">
                  {formatMoney({ amount: moneyValue(order.total), currency: "COP" })}
                </td>
                <td className="px-4 py-2.5 text-sf-muted">
                  {order.paymentId ? shortRef(order.paymentId) : "—"}
                </td>
                <td className="px-4 py-2.5 text-sf-muted">
                  {formatAdminInstant(order.createdAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <HistoryPagination
        page={page}
        size={10}
        totalElements={query.data?.totalElements ?? 0}
        singular="pedido"
        plural="pedidos"
        onPageChange={onPageChange}
      />
    </Card>
  );
}

function PaymentsTab({
  query,
  page,
  onPageChange,
  ordersById,
  orderNumbers,
  accountEmails,
}: {
  query: {
    isPending: boolean;
    isError: boolean;
    data?:
      | {
          items: {
            id: string;
            orderId: string;
            amount: { amount: number | string };
            paymentMethod: PaymentMethod;
            status: PaymentStatus;
            refundedAt: string | null;
            createdAt: string;
          }[];
          totalElements: number;
        }
      | undefined;
    refetch: () => void;
  };
  page: number;
  onPageChange: (page: number) => void;
  ordersById: Map<string, string>;
  orderNumbers: Map<string, string>;
  accountEmails: Map<string, string>;
}) {
  if (query.isPending) {
    return (
      <Card className="p-0">
        <div className="space-y-2 p-4" aria-hidden="true">
          {[0, 1, 2, 3].map((row) => (
            <Skeleton key={row} className="h-9 w-full" />
          ))}
        </div>
      </Card>
    );
  }
  if (query.isError) {
    return (
      <Card className="p-4">
        <Alert tone="error" title="No se pudo cargar el historial de pagos">
          <button
            type="button"
            onClick={() => query.refetch()}
            className="font-semibold underline underline-offset-2"
          >
            Reintentar
          </button>
        </Alert>
      </Card>
    );
  }
  const items = query.data?.items ?? [];
  if (items.length === 0) {
    return (
      <Card className="p-0">
        <EmptyState
          title="Este cliente no tiene pagos registrados."
          description="Los pagos aparecerán cuando existan pedidos confirmados."
        />
      </Card>
    );
  }
  return (
    <Card className="overflow-hidden p-0">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[56rem] text-left text-sm">
          <thead>
            <tr className="border-b border-sf-border bg-sf-bg/60 text-xs font-medium text-sf-muted">
              <th className="px-4 py-2 font-medium">Pago</th>
              <th className="px-4 py-2 font-medium">Pedido</th>
              <th className="px-4 py-2 font-medium">Cuenta</th>
              <th className="px-4 py-2 font-medium">Método</th>
              <th className="px-4 py-2 text-right font-medium">Monto</th>
              <th className="px-4 py-2 font-medium">Estado</th>
              <th className="px-4 py-2 font-medium">Reembolso</th>
              <th className="px-4 py-2 font-medium">Fecha</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-sf-border/80">
            {items.map((payment) => {
              const orderCustomerId = ordersById.get(payment.orderId);
              return (
                <tr
                  key={payment.id}
                  className="transition-colors duration-150 hover:bg-sf-bg/60"
                >
                  <td className="px-4 py-2.5 font-semibold text-sf-ink">
                    {shortRef(payment.id)}
                  </td>
                  <td className="px-4 py-2.5 text-sf-muted">
                    {orderNumbers.get(payment.orderId) ?? shortRef(payment.orderId)}
                  </td>
                  <td className="px-4 py-2.5 text-sf-muted">
                    {orderCustomerId != null
                      ? (accountEmails.get(orderCustomerId) ?? "—")
                      : "—"}
                  </td>
                  <td className="px-4 py-2.5 text-sf-ink">
                    {paymentMethodLabel(payment.paymentMethod)}
                  </td>
                  <td className="px-4 py-2.5 text-right font-semibold tabular-nums text-sf-ink">
                    {formatMoney({ amount: moneyValue(payment.amount), currency: "COP" })}
                  </td>
                  <td className="px-4 py-2.5">
                    <span
                      className={cx(
                        "inline-flex items-center rounded-lg px-2 py-0.5 text-[11px] font-semibold",
                        adminCustomerPaymentStatusToneClass(payment.status),
                      )}
                    >
                      {adminCustomerPaymentStatusLabel(payment.status)}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-sf-muted">
                    {payment.refundedAt
                      ? formatAdminInstant(payment.refundedAt)
                      : "—"}
                  </td>
                  <td className="px-4 py-2.5 text-sf-muted">
                    {formatAdminInstant(payment.createdAt)}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <HistoryPagination
        page={page}
        size={10}
        totalElements={query.data?.totalElements ?? 0}
        singular="pago"
        plural="pagos"
        onPageChange={onPageChange}
      />
    </Card>
  );
}

function HistoryPagination({
  page,
  size,
  totalElements,
  singular,
  plural,
  onPageChange,
}: {
  page: number;
  size: number;
  totalElements: number;
  singular: string;
  plural: string;
  onPageChange: (page: number) => void;
}) {
  return (
    <div className="flex items-center justify-between gap-3 border-t border-sf-border px-4 py-2.5">
      <p className="text-xs text-sf-muted">
        {adminCustomerPageRangeLabel(page, size, totalElements, singular, plural)}
      </p>
      <div className="flex items-center gap-1.5">
        <button
          type="button"
          aria-label="Página anterior"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
          className={cx(
            "inline-flex h-8 min-w-8 items-center justify-center rounded-lg border border-sf-border px-2 text-sm font-semibold text-sf-ink transition-colors",
            page === 0 ? "cursor-not-allowed opacity-50" : "hover:bg-sf-bg",
          )}
        >
          ‹
        </button>
        <span className="inline-flex h-8 min-w-8 items-center justify-center rounded-lg bg-sf-primary/10 px-2 text-sm font-semibold text-sf-primary">
          {page + 1}
        </span>
        <button
          type="button"
          aria-label="Página siguiente"
          disabled={(page + 1) * size >= totalElements}
          onClick={() => onPageChange(page + 1)}
          className={cx(
            "inline-flex h-8 min-w-8 items-center justify-center rounded-lg border border-sf-border px-2 text-sm font-semibold text-sf-ink transition-colors",
            (page + 1) * size >= totalElements
              ? "cursor-not-allowed opacity-50"
              : "hover:bg-sf-bg",
          )}
        >
          ›
        </button>
      </div>
    </div>
  );
}

function CommercialInfoTab({
  record,
}: {
  record: {
    documentType: string;
    documentNumber: string;
    billingFirstName: string;
    billingLastName: string;
    createdAt: string;
    updatedAt: string;
    accounts: { status: "ACTIVE" | "INACTIVE"; deletedAt: string | null }[];
  };
}) {
  const summary = adminCustomerAccountsSummary(record.accounts);
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card className="p-4">
        <h3 className="text-base font-semibold text-sf-ink">
          Información comercial
        </h3>
        <dl className="mt-3 grid gap-2 text-sm">
          <div className="flex justify-between gap-3">
            <dt className="text-sf-muted">Nombre de facturación</dt>
            <dd className="text-right font-semibold text-sf-ink">
              {record.billingFirstName} {record.billingLastName}
            </dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-sf-muted">Tipo de documento</dt>
            <dd className="text-right text-sf-ink">
              {adminCustomerDocumentTypeLabel(record.documentType)}
            </dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-sf-muted">Número de documento</dt>
            <dd className="text-right tabular-nums text-sf-ink">
              {record.documentNumber}
            </dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-sf-muted">Fecha de creación</dt>
            <dd className="text-right text-sf-ink">
              {formatAdminInstant(record.createdAt)}
            </dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-sf-muted">Última actualización</dt>
            <dd className="text-right text-sf-ink">
              {formatAdminInstant(record.updatedAt)}
            </dd>
          </div>
        </dl>
      </Card>
      <Card className="p-4">
        <h3 className="text-base font-semibold text-sf-ink">Cuentas</h3>
        <dl className="mt-3 grid gap-2 text-sm">
          <div className="flex justify-between gap-3">
            <dt className="text-sf-muted">Cuentas asociadas</dt>
            <dd className="text-right font-semibold text-sf-ink">
              {summary.total}
            </dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-sf-muted">Cuentas activas</dt>
            <dd className="text-right font-semibold text-emerald-700">
              {summary.active}
            </dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-sf-muted">Cuentas inactivas</dt>
            <dd className="text-right font-semibold text-sf-ink">
              {summary.inactive}
            </dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-sf-muted">Cuentas eliminadas</dt>
            <dd className="text-right font-semibold text-sf-ink">
              {summary.deleted}
            </dd>
          </div>
        </dl>
      </Card>
    </div>
  );
}
