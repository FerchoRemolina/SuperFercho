"use client";

import Link from "next/link";
import {
  paymentMethodLabel,
  paymentStatusLabel,
} from "@/features/orders/api";
import { OrderStatusBadge } from "@/features/orders/components/order-status-badge";
import { CancelOrderPanel } from "@/features/orders/components/cancel-order-panel";
import { useOrderQuery } from "@/features/orders/hooks";
import {
  formatOrderDate,
  orderDetailView,
  paymentRefundLabel,
} from "@/features/orders/order-views";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { Skeleton } from "@/shared/ui/skeleton";

export function OrderResultContent({ orderId }: { orderId: string }) {
  const orderQuery = useOrderQuery(orderId);
  const view = orderDetailView(orderQuery);

  if (view.kind === "loading") {
    return (
      <Container as="main" className="py-8 md:py-16">
        <Skeleton className="mb-3 h-5 w-24 md:mb-4" />
        <Skeleton className="h-9 w-48 md:h-11" />
        <OrderDetailSkeleton />
      </Container>
    );
  }

  if (view.kind === "error") {
    return (
      <Container as="main" className="py-8 md:py-16">
        <Link
          href="/orders"
          className="mb-3 inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:text-sf-primary md:mb-4"
        >
          ← Pedidos
        </Link>
        <div className="mt-2 grid gap-4">
          <Alert tone="error" title={view.title}>
            {view.message}
          </Alert>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Link href="/orders" className={buttonClassName("secondary")}>
              Volver a mis pedidos
            </Link>
            <Button
              type="button"
              variant="secondary"
              onClick={() => orderQuery.refetch()}
            >
              Reintentar
            </Button>
          </div>
        </div>
      </Container>
    );
  }

  const order = view.order;
  const payment = order.payment;
  const address = order.shippingAddress;
  const refundLabel = paymentRefundLabel(payment);

  return (
    <Container as="main" className="py-8 md:py-16">
      <Link
        href="/orders"
        className="mb-3 inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:text-sf-primary md:mb-4"
      >
        ← Pedidos
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-3">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          {order.orderNumber}
        </h1>
        <OrderStatusBadge status={order.status} showHint />
      </div>

      <p className="mt-3 text-2xl font-bold text-sf-ink md:text-3xl">
        {formatMoney(order.total)}
      </p>
      <p className="mt-1 text-sm text-sf-muted">
        {formatOrderDate(order.createdAt)}
      </p>

      <div className="mt-6 grid gap-4 md:mt-8 md:gap-5">
        <Card className="grid gap-3 !p-4 md:gap-4 md:!p-5">
          <h2 className="text-lg font-semibold text-sf-ink md:text-xl">
            Productos
          </h2>
          <ul className="grid gap-2.5">
            {order.items.map((item) => (
              <li
                key={item.id}
                className="grid grid-cols-[minmax(0,1fr)_auto] items-start gap-3 border-b border-sf-border pb-2.5 last:border-b-0 last:pb-0"
              >
                <div className="min-w-0">
                  <p className="line-clamp-2 text-sm font-semibold leading-snug text-sf-ink md:text-base">
                    {item.productName}
                  </p>
                  <p className="mt-1 text-sm font-medium text-sf-ink">
                    Cantidad: {item.quantity}
                  </p>
                  <p className="mt-0.5 text-xs text-sf-muted">
                    {formatMoney(item.unitPrice)} c/u
                  </p>
                </div>
                <p className="pt-0.5 text-sm font-bold text-sf-ink md:text-base">
                  {formatMoney(item.subtotal)}
                </p>
              </li>
            ))}
          </ul>
        </Card>

        <Card className="grid gap-3 !p-4 md:gap-3 md:!p-5">
          <h2 className="text-lg font-semibold text-sf-ink md:text-xl">
            Resumen
          </h2>
          <dl className="grid gap-2 text-sm">
            <div className="flex justify-between gap-4">
              <dt className="text-sf-muted">Subtotal</dt>
              <dd className="font-semibold text-sf-ink">
                {formatMoney(order.subtotal)}
              </dd>
            </div>
            <div className="flex justify-between gap-4 border-t border-sf-border pt-3">
              <dt className="text-base font-semibold text-sf-ink">Total</dt>
              <dd className="text-lg font-bold text-sf-ink">
                {formatMoney(order.total)}
              </dd>
            </div>
          </dl>

          {payment ? (
            <div className="grid gap-1.5 border-t border-sf-border pt-3 text-sm">
              <p className="text-xs font-medium uppercase tracking-wide text-sf-muted">
                Pago
              </p>
              <p className="text-sf-ink">
                <span className="font-semibold">
                  {paymentMethodLabel(payment.paymentMethod)}
                </span>
                <span className="text-sf-muted">
                  {" "}
                  · {paymentStatusLabel(payment.status)}
                </span>
              </p>
              <p className="text-sm text-sf-muted">
                Monto: {formatMoney(payment.amount)}
              </p>
              {refundLabel ? (
                <p className="text-xs text-sf-muted">{refundLabel}</p>
              ) : null}
            </div>
          ) : (
            <p className="border-t border-sf-border pt-3 text-sm text-sf-muted">
              No hay información de pago disponible para este pedido.
            </p>
          )}

          {(order.confirmedAt || order.cancelledAt) && (
            <div className="grid gap-1 border-t border-sf-border pt-3 text-xs text-sf-muted">
              {order.confirmedAt ? (
                <p>Confirmado: {formatOrderDate(order.confirmedAt)}</p>
              ) : null}
              {order.cancelledAt ? (
                <p>Cancelado: {formatOrderDate(order.cancelledAt)}</p>
              ) : null}
            </div>
          )}
        </Card>

        <Card className="grid gap-2 !p-4 md:!p-5">
          <h2 className="text-lg font-semibold text-sf-ink md:text-xl">
            Entrega
          </h2>
          <p className="font-semibold text-sf-ink">{address.recipientName}</p>
          <p className="text-sm text-sf-muted">
            {address.addressLine}
            {address.additionalInfo ? ` · ${address.additionalInfo}` : ""}
          </p>
          <p className="text-sm text-sf-muted">
            {address.city}, {address.department}
          </p>
          <p className="text-sm text-sf-muted">{address.phone}</p>
        </Card>

        <CancelOrderPanel order={order} />

        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:gap-4">
          <Link
            href="/orders"
            className={buttonClassName("secondary")}
          >
            Volver a mis pedidos
          </Link>
          <Link
            href="/catalog"
            className="inline-flex min-h-11 items-center justify-center text-sm font-semibold text-sf-muted hover:text-sf-primary hover:underline"
          >
            Seguir comprando
          </Link>
        </div>
      </div>
    </Container>
  );
}

function OrderDetailSkeleton() {
  return (
    <div className="mt-6 grid gap-4 md:mt-8" aria-hidden="true">
      <Skeleton className="h-7 w-36" />
      <Skeleton className="h-4 w-44" />
      <Card className="grid gap-3 !p-4">
        <Skeleton className="h-5 w-24" />
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-12 w-full" />
      </Card>
      <Card className="grid gap-3 !p-4">
        <Skeleton className="h-5 w-28" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-2/3" />
      </Card>
      <Card className="grid gap-2 !p-4">
        <Skeleton className="h-5 w-20" />
        <Skeleton className="h-4 w-48" />
        <Skeleton className="h-4 w-56" />
      </Card>
    </div>
  );
}
