"use client";

import Link from "next/link";
import { PaymentStatusBadge } from "@/features/admin/components/payment-status-badge";
import { useAdminPaymentQuery } from "@/features/admin/hooks";
import {
  adminOrderDetailHref,
  adminOrdersHref,
  adminPaymentDetailErrorKind,
  formatAdminInstant,
} from "@/features/admin/presentation";
import { paymentMethodLabel } from "@/features/orders/api";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { BackLink } from "@/shared/ui/back-link";
import { Card } from "@/shared/ui/card";
import { EmptyState } from "@/shared/ui/empty-state";
import { CartIcon, ChevronIcon } from "@/shared/ui/icons";
import { Skeleton } from "@/shared/ui/skeleton";

function DetailField({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs text-sf-muted">{label}</dt>
      <dd className="mt-0.5 break-words text-sm font-medium text-sf-ink">
        {value}
      </dd>
    </div>
  );
}

export function AdminPaymentDetailPage({ paymentId }: { paymentId: string }) {
  const paymentQuery = useAdminPaymentQuery(paymentId);

  if (paymentQuery.isPending) {
    return (
      <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-40 w-full" />
        <Skeleton className="mt-4 h-32 w-full" />
      </main>
    );
  }

  if (paymentQuery.isError) {
    const kind = adminPaymentDetailErrorKind(paymentQuery.error);
    const message = isApiError(paymentQuery.error)
      ? messageForApiProblem(paymentQuery.error.problem)
      : "No se pudo completar la solicitud.";

    if (kind === "payment_not_found") {
      return (
        <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
          <EmptyState
            title="No encontramos este pago"
            description={message}
            action={
              <Link
                href={adminOrdersHref({})}
                className={buttonClassName("secondary")}
              >
                Volver a pedidos
              </Link>
            }
          />
        </main>
      );
    }

    return (
      <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
        <Alert tone="error" title="No se pudo cargar el pago">
          {message}
        </Alert>
        <div className="mt-4 flex flex-col gap-2 sm:flex-row">
          <Link
            href={adminOrdersHref({})}
            className={buttonClassName("secondary")}
          >
            Volver a pedidos
          </Link>
          <Button
            type="button"
            variant="secondary"
            onClick={() => void paymentQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      </main>
    );
  }

  const payment = paymentQuery.data;
  if (!payment) {
    return (
      <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-40 w-full" />
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
      <BackLink href={adminOrdersHref({})} className="mb-4">
        Volver a pedidos
      </BackLink>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-[0.08em] text-sf-muted">
            Pagos
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
            {paymentMethodLabel(payment.paymentMethod)}
          </h1>
          <p className="mt-2 text-sm text-sf-muted">
            Creado {formatAdminInstant(payment.createdAt)} · Actualizado{" "}
            {formatAdminInstant(payment.updatedAt)}
          </p>
        </div>
        <PaymentStatusBadge status={payment.status} />
      </div>

      {payment.refundedAt ? (
        <div className="mt-6">
          <Alert tone="info" title="Pago reembolsado">
            Este pago permanece Aprobado. Fue reembolsado el{" "}
            {formatAdminInstant(payment.refundedAt)}.
          </Alert>
        </div>
      ) : null}

      <Card className="mt-6 grid content-start gap-3.5 p-4 md:p-5">
        <div className="flex items-start gap-3">
          <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-sf-primary/10 text-sf-primary">
            <CartIcon className="h-5 w-5" />
          </span>
          <div className="min-w-0">
            <h2 className="text-lg font-semibold text-sf-ink">Resumen</h2>
          </div>
        </div>
        <dl className="grid gap-x-6 gap-y-3.5 sm:grid-cols-2">
          <div className="min-w-0">
            <dt className="text-xs text-sf-muted">Id de pago</dt>
            <dd className="mt-0.5 break-all text-xs text-sf-muted">
              {payment.id}
            </dd>
          </div>
          <div className="min-w-0">
            <dt className="text-xs text-sf-muted">Pedido</dt>
            <dd className="mt-0.5">
              <Link
                href={adminOrderDetailHref(payment.orderId)}
                className="inline-flex items-center gap-1 text-sm font-medium text-sf-primary underline-offset-2 hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary"
              >
                Ver pedido asociado
                <ChevronIcon className="h-3.5 w-3.5 shrink-0" />
              </Link>
            </dd>
          </div>
          <DetailField
            label="Método"
            value={paymentMethodLabel(payment.paymentMethod)}
          />
          <div>
            <dt className="text-xs text-sf-muted">Estado</dt>
            <dd className="mt-1">
              <PaymentStatusBadge status={payment.status} />
            </dd>
          </div>
          <div className="self-start rounded-lg bg-sf-bg px-3 py-2">
            <dt className="text-xs text-sf-muted">Monto</dt>
            <dd className="mt-0.5 text-lg font-bold tabular-nums text-sf-ink">
              {formatMoney(payment.amount)}
            </dd>
          </div>
          <DetailField label="Moneda" value={payment.amount.currency} />
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
          {payment.refundedAt ? (
            <DetailField
              label="Reembolsado"
              value={formatAdminInstant(payment.refundedAt)}
            />
          ) : null}
        </dl>
      </Card>
    </main>
  );
}
