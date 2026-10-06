"use client";

import Link from "next/link";
import { Fragment, type ReactNode } from "react";
import { useAdminOrderQuery } from "@/features/admin/hooks";
import {
  adminOrderDetailErrorKind,
  adminPaymentHref,
  formatAdminInstant,
} from "@/features/admin/presentation";
import {
  paymentMethodLabel,
  type OrderStatus,
} from "@/features/orders/api";
import { OrderStatusBadge } from "@/features/orders/components/order-status-badge";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { BackLink } from "@/shared/ui/back-link";
import { Card } from "@/shared/ui/card";
import { EmptyState } from "@/shared/ui/empty-state";
import {
  CartIcon,
  ChevronIcon,
  EyeIcon,
  MapPinIcon,
  UserIcon,
} from "@/shared/ui/icons";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";
import { PaymentStatusBadge } from "@/features/admin/components/payment-status-badge";

const STATUS_STEP_INDEX: Record<OrderStatus, number> = {
  CONFIRMED: 0,
  PREPARING: 1,
  DELIVERY: 2,
  DELIVERED: 3,
  CANCELLED: -1,
};

type TimelineStep = {
  label: string;
  timestamp: string | null;
  reached: boolean;
  current: boolean;
  danger: boolean;
};

function lifecycleTimeline(
  status: OrderStatus,
  createdAt: string,
  confirmedAt: string | null,
): TimelineStep[] {
  const reachedIndex = STATUS_STEP_INDEX[status];
  return [
    {
      label: "Creado",
      timestamp: formatAdminInstant(createdAt),
      reached: true,
      current: reachedIndex < 0,
      danger: reachedIndex < 0,
    },
    {
      label: "Confirmado",
      timestamp: confirmedAt ? formatAdminInstant(confirmedAt) : null,
      reached: reachedIndex >= 0,
      current: reachedIndex === 0,
      danger: false,
    },
    {
      label: "En preparación",
      timestamp: null,
      reached: reachedIndex >= 1,
      current: reachedIndex === 1,
      danger: false,
    },
    {
      label: "En camino",
      timestamp: null,
      reached: reachedIndex >= 2,
      current: reachedIndex === 2,
      danger: false,
    },
    {
      label: "Entregado",
      timestamp: null,
      reached: reachedIndex >= 3,
      current: reachedIndex === 3,
      danger: false,
    },
  ];
}

function cancelledTimeline(
  createdAt: string,
  cancelledAt: string | null,
): TimelineStep[] {
  return [
    {
      label: "Creado",
      timestamp: formatAdminInstant(createdAt),
      reached: true,
      current: false,
      danger: false,
    },
    {
      label: "Cancelado",
      timestamp: cancelledAt ? formatAdminInstant(cancelledAt) : null,
      reached: true,
      current: true,
      danger: true,
    },
  ];
}

function StepCircle({ step }: { step: TimelineStep }) {
  if (step.current) {
    return (
      <span
        className={cx(
          "inline-flex h-7 w-7 items-center justify-center rounded-full",
          step.danger ? "bg-red-100" : "bg-sf-primary/15",
        )}
      >
        <span
          className={cx(
            "inline-flex h-3.5 w-3.5 rounded-full",
            step.danger ? "bg-red-600" : "bg-sf-primary",
          )}
        />
      </span>
    );
  }
  if (step.reached) {
    return (
      <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-sf-primary">
        <svg
          viewBox="0 0 12 10"
          className="h-3 w-3 text-white"
          fill="none"
          aria-hidden="true"
        >
          <path
            d="M1 5.2 4.2 8.4 11 1.6"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
    );
  }
  return (
    <span className="inline-flex h-7 w-7 items-center justify-center rounded-full border border-sf-border bg-sf-surface">
      <span className="inline-flex h-3 w-3 rounded-full bg-sf-border" />
    </span>
  );
}

function StepLabels({
  step,
  align = "center",
}: {
  step: TimelineStep;
  align?: "center" | "left";
}) {
  return (
    <div
      className={cx(
        "mt-2 flex flex-col gap-1",
        align === "center" ? "items-center text-center" : "items-start",
      )}
    >
      <p
        className={cx(
          "text-sm font-semibold",
          step.current
            ? step.danger
              ? "text-red-700"
              : "text-sf-primary"
            : step.reached
              ? "text-sf-ink"
              : "text-sf-muted",
        )}
      >
        {step.label}
      </p>
      {step.current ? (
        <span
          className={cx(
            "rounded-md px-1.5 py-0.5 text-[11px] font-semibold",
            step.danger
              ? "bg-red-50 text-red-700"
              : "bg-sf-primary/10 text-sf-primary",
          )}
        >
          Actual
        </span>
      ) : null}
      {step.timestamp ? (
        <p className="text-xs tabular-nums text-sf-muted">{step.timestamp}</p>
      ) : null}
    </div>
  );
}

function ProcessTimeline({
  steps,
}: {
  steps: TimelineStep[];
}) {
  return (
    <>
      <ol className="hidden md:flex md:items-start">
        {steps.map((step, index) => (
          <Fragment key={step.label}>
            {index > 0 ? (
              <li
                aria-hidden="true"
                className={cx(
                  "mt-3.5 h-0.5 min-w-6 flex-1",
                  step.reached ? "bg-sf-primary" : "bg-sf-border",
                )}
              />
            ) : null}
            <li className="flex w-24 flex-none flex-col items-center sm:w-28">
              <StepCircle step={step} />
              <StepLabels step={step} />
            </li>
          </Fragment>
        ))}
      </ol>
      <ol className="grid md:hidden">
        {steps.map((step, index) => (
          <li key={step.label} className="flex gap-3">
            <div className="flex flex-col items-center">
              <StepCircle step={step} />
              {index < steps.length - 1 ? (
                <span
                  aria-hidden="true"
                  className={cx(
                    "my-1 w-0.5 flex-1",
                    steps[index + 1]?.reached ? "bg-sf-primary" : "bg-sf-border",
                  )}
                />
              ) : null}
            </div>
            <div className={cx("pb-4", index === steps.length - 1 && "pb-0")}>
              <StepLabels step={step} align="left" />
            </div>
          </li>
        ))}
      </ol>
    </>
  );
}

function CardTitle({
  icon: Icon,
  title,
  subtitle,
}: {
  icon: (props: { className?: string }) => ReactNode;
  title: string;
  subtitle?: string;
}) {
  return (
    <div className="flex items-start gap-3">
      <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-sf-primary/10 text-sf-primary">
        <Icon className="h-5 w-5" />
      </span>
      <div className="min-w-0">
        <h2 className="text-lg font-semibold text-sf-ink">{title}</h2>
        {subtitle ? (
          <p className="mt-0.5 text-xs text-sf-muted">{subtitle}</p>
        ) : null}
      </div>
    </div>
  );
}

function DetailField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-sf-muted">{label}</dt>
      <dd className="mt-0.5 break-words text-sm font-medium text-sf-ink">
        {value}
      </dd>
    </div>
  );
}

export function AdminOrderDetailPageContent({
  orderId,
}: {
  orderId: string;
}) {
  const orderQuery = useAdminOrderQuery(orderId);

  if (orderQuery.isPending) {
    return (
      <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-40 w-full" />
        <Skeleton className="mt-4 h-32 w-full" />
      </main>
    );
  }

  if (orderQuery.isError) {
    const kind = adminOrderDetailErrorKind(orderQuery.error);
    const message = isApiError(orderQuery.error)
      ? messageForApiProblem(orderQuery.error.problem)
      : "No se pudo completar la solicitud.";

    if (kind === "order_not_found") {
      return (
        <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
          <EmptyState
            title="No encontramos este pedido"
            description={message}
            action={
              <Link
                href="/admin/orders"
                className={buttonClassName("secondary")}
              >
                Volver al listado
              </Link>
            }
          />
        </main>
      );
    }

    return (
      <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
        <Alert tone="error" title="No se pudo cargar el pedido">
          {message}
        </Alert>
        <div className="mt-4 flex flex-col gap-2 sm:flex-row">
          <Link href="/admin/orders" className={buttonClassName("secondary")}>
            Volver al listado
          </Link>
          <Button
            type="button"
            variant="secondary"
            onClick={() => void orderQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      </main>
    );
  }

  const order = orderQuery.data;
  if (!order) {
    return (
      <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-40 w-full" />
      </main>
    );
  }

  const payment = order.payment;
  const address = order.shippingAddress;
  const customer = order.customer;
  const steps =
    order.status === "CANCELLED"
      ? cancelledTimeline(order.createdAt, order.cancelledAt)
      : lifecycleTimeline(order.status, order.createdAt, order.confirmedAt);
  const refunded = payment?.refundedAt
    ? `Reembolsado: ${formatAdminInstant(payment.refundedAt)}`
    : null;

  return (
    <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
      <BackLink href="/admin/orders" className="mb-4">Volver al listado</BackLink>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-[0.08em] text-sf-muted">
            Ventas · Pedidos
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight tabular-nums text-sf-ink md:text-3xl">
            {order.orderNumber}
          </h1>
          <p className="mt-2 text-sm text-sf-muted">
            Creado {formatAdminInstant(order.createdAt)} · Actualizado{" "}
            {formatAdminInstant(order.updatedAt)}
          </p>
        </div>
        <OrderStatusBadge status={order.status} />
      </div>

      <Card className="mt-6 p-4 md:p-6">
        <h2 className="text-xl font-semibold text-sf-ink">
          Proceso del pedido
        </h2>
        <p className="mt-1 text-sm text-sf-muted">
          Sigue el estado actual y el historial de este pedido.
        </p>
        <div className="mt-6">
          <ProcessTimeline steps={steps} />
        </div>
      </Card>

      <Card className="mt-6 p-4 md:p-6">
        <h2 className="text-xl font-semibold text-sf-ink">Productos</h2>
        <div className="mt-4 overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-sf-border bg-sf-bg/60 text-xs font-medium text-sf-muted">
                <th className="rounded-l-lg px-4 py-2.5 font-medium">Producto</th>
                <th className="px-4 py-2.5 text-center font-medium">Cantidad</th>
                <th className="px-4 py-2.5 text-right font-medium">
                  Precio unitario
                </th>
                <th className="rounded-r-lg px-4 py-2.5 text-right font-medium">
                  Subtotal
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sf-border/80">
              {order.items.map((item) => (
                <tr key={item.id}>
                  <td className="px-4 py-3 font-medium text-sf-ink">
                    {item.productName}
                  </td>
                  <td className="px-4 py-3 text-center tabular-nums text-sf-ink">
                    {item.quantity}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums text-sf-ink">
                    {formatMoney(item.unitPrice)}
                  </td>
                  <td className="px-4 py-3 text-right font-semibold tabular-nums text-sf-ink">
                    {formatMoney(item.subtotal)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-4 flex items-center justify-between gap-4 border-t border-sf-border px-4 pt-3">
          <p className="text-sm font-semibold text-sf-ink">Total</p>
          <p className="text-lg font-bold tabular-nums text-sf-ink">
            {formatMoney(order.total)}
          </p>
        </div>
      </Card>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <Card className="grid content-start gap-4 p-4 md:p-5">
          <CardTitle icon={UserIcon} title="Cliente" />
          {customer ? (
            <dl className="grid gap-4">
              <DetailField label="Nombre" value={customer.fullName ?? "—"} />
              {customer.documentType && customer.documentNumber ? (
                <DetailField
                  label="Documento"
                  value={`${customer.documentType} ${customer.documentNumber}`}
                />
              ) : null}
              {customer.email ? (
                <DetailField label="Email" value={customer.email} />
              ) : null}
              {customer.phone ? (
                <DetailField label="Teléfono" value={customer.phone} />
              ) : null}
            </dl>
          ) : (
            <p className="rounded-lg bg-sf-bg px-3 py-2 text-sm text-sf-muted">
              Cliente no disponible
            </p>
          )}
        </Card>

        <Card className="grid content-start gap-4 p-4 md:p-5">
          <CardTitle
            icon={MapPinIcon}
            title="Destinatario"
            subtitle="Snapshot de envío al momento del pedido."
          />
          <dl className="grid gap-4">
            <DetailField label="Nombre" value={address.recipientName} />
            <DetailField label="Teléfono" value={address.phone} />
            <div>
              <dt className="text-xs text-sf-muted">Dirección</dt>
              <dd className="mt-0.5 text-sm font-medium text-sf-ink">
                {address.addressLine}
                {address.additionalInfo ? ` · ${address.additionalInfo}` : ""}
              </dd>
            </div>
            <DetailField
              label="Ciudad / Departamento"
              value={`${address.city}, ${address.department}`}
            />
          </dl>
        </Card>

        <Card className="grid content-start overflow-hidden p-0">
          <div className="grid content-start gap-4 p-4 md:p-5">
            <CardTitle icon={CartIcon} title="Pago" />
            {payment ? (
              <>
                <dl className="grid grid-cols-2 gap-x-4 gap-y-4">
                  <DetailField
                    label="Método"
                    value={paymentMethodLabel(payment.paymentMethod)}
                  />
                  <div>
                    <dt className="text-xs text-sf-muted">Estado del pago</dt>
                    <dd className="mt-1">
                      <PaymentStatusBadge status={payment.status} />
                    </dd>
                  </div>
                  <DetailField
                    label="Monto"
                    value={formatMoney(payment.amount)}
                  />
                  {payment.providerReference ? (
                    <DetailField
                      label="Referencia"
                      value={payment.providerReference}
                    />
                  ) : null}
                  <DetailField
                    label="Creado"
                    value={formatAdminInstant(payment.createdAt)}
                  />
                  <DetailField
                    label="Actualizado"
                    value={formatAdminInstant(payment.updatedAt)}
                  />
                </dl>
                {payment.status === "PENDING" ? (
                  <p className="text-sm text-sf-muted">Pago contra entrega</p>
                ) : null}
                {refunded ? (
                  <p className="text-sm font-medium text-sf-ink">{refunded}</p>
                ) : null}
              </>
            ) : (
              <p className="rounded-lg bg-sf-bg px-3 py-2 text-sm text-sf-muted">
                Pago no disponible
              </p>
            )}
          </div>
          {payment ? (
            <Link
              href={adminPaymentHref(payment.paymentId)}
              className={cx(
                "flex w-full items-center gap-2 bg-sf-primary px-4 py-3.5 text-sm font-semibold text-white",
                "transition-colors hover:bg-sf-primary/90",
                "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white",
              )}
            >
              <EyeIcon className="h-4 w-4 shrink-0" />
              Ver detalles del pago
              <ChevronIcon className="ml-auto h-4 w-4 shrink-0" />
            </Link>
          ) : null}
        </Card>
      </div>
    </main>
  );
}
