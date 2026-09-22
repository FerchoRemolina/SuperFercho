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
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminPaymentDetailPage({ paymentId }: { paymentId: string }) {
  const paymentQuery = useAdminPaymentQuery(paymentId);

  if (paymentQuery.isPending) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-40 w-full" />
        <Skeleton className="mt-4 h-32 w-full" />
      </Container>
    );
  }

  if (paymentQuery.isError) {
    const kind = adminPaymentDetailErrorKind(paymentQuery.error);
    const message = isApiError(paymentQuery.error)
      ? messageForApiProblem(paymentQuery.error.problem)
      : "No se pudo completar la solicitud.";

    if (kind === "payment_not_found") {
      return (
        <Container as="main" className="py-10 md:py-16">
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
        </Container>
      );
    }

    return (
      <Container as="main" className="py-10 md:py-16">
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
      </Container>
    );
  }

  const payment = paymentQuery.data;
  if (!payment) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-40 w-full" />
      </Container>
    );
  }

  return (
    <Container as="main" className="py-10 md:py-16">
      <Link
        href={adminOrdersHref({})}
        className={`${buttonClassName("ghost")} mb-4 px-0`}
      >
        Volver a pedidos
      </Link>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-sf-muted">Pago</p>
          <h1 className="mt-1 break-all text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            {payment.id}
          </h1>
          <p className="mt-2 text-base text-sf-muted">
            {paymentMethodLabel(payment.paymentMethod)}
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

      <div className="mt-8 grid gap-6">
        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Resumen</h2>
          <dl className="grid gap-3 text-sm md:grid-cols-2">
            <div>
              <dt className="text-sf-muted">Id de pago</dt>
              <dd className="mt-1 break-all font-semibold text-sf-ink">
                {payment.id}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Pedido</dt>
              <dd className="mt-1 break-all font-semibold text-sf-ink">
                <Link
                  href={adminOrderDetailHref(payment.orderId)}
                  className="text-sf-primary underline-offset-2 hover:underline"
                >
                  {payment.orderId}
                </Link>
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Método</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {paymentMethodLabel(payment.paymentMethod)}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Estado</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                <PaymentStatusBadge status={payment.status} />
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Monto</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {formatMoney(payment.amount)}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Moneda</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {payment.amount.currency}
              </dd>
            </div>
            {payment.providerReference ? (
              <div>
                <dt className="text-sf-muted">Referencia</dt>
                <dd className="mt-1 break-all font-semibold text-sf-ink">
                  {payment.providerReference}
                </dd>
              </div>
            ) : null}
            <div>
              <dt className="text-sf-muted">Creado</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {formatAdminInstant(payment.createdAt)}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Actualizado</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {formatAdminInstant(payment.updatedAt)}
              </dd>
            </div>
            {payment.refundedAt ? (
              <div>
                <dt className="text-sf-muted">Reembolsado</dt>
                <dd className="mt-1 font-semibold text-sf-ink">
                  {formatAdminInstant(payment.refundedAt)}
                </dd>
              </div>
            ) : null}
          </dl>
        </Card>
      </div>
    </Container>
  );
}
