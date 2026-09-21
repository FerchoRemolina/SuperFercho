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
      <Container as="main" className="py-10 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Pedido
        </h1>
        <div className="mt-8 grid gap-4">
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-32 w-full" />
        </div>
      </Container>
    );
  }

  if (view.kind === "error") {
    return (
      <Container as="main" className="py-10 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          {view.title}
        </h1>
        <div className="mt-8 grid gap-4">
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
    <Container as="main" className="py-10 md:py-16">
      <p className="text-sm font-semibold text-sf-muted">Pedido</p>
      <h1 className="mt-1 text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        {order.orderNumber}
      </h1>
      <div className="mt-3">
        <OrderStatusBadge status={order.status} showHint />
      </div>

      <div className="mt-8 grid gap-6">
        <Card className="grid gap-4">
          <dl className="grid gap-3 text-sm md:grid-cols-2">
            <div>
              <dt className="text-sf-muted">Número de pedido</dt>
              <dd className="mt-1 text-lg font-semibold text-sf-ink">
                {order.orderNumber}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Total</dt>
              <dd className="mt-1 text-lg font-bold text-sf-ink">
                {formatMoney(order.total)}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Subtotal</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {formatMoney(order.subtotal)}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Creado</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {formatOrderDate(order.createdAt)}
              </dd>
            </div>
            {order.confirmedAt ? (
              <div>
                <dt className="text-sf-muted">Confirmado</dt>
                <dd className="mt-1 font-semibold text-sf-ink">
                  {formatOrderDate(order.confirmedAt)}
                </dd>
              </div>
            ) : null}
            {order.cancelledAt ? (
              <div>
                <dt className="text-sf-muted">Cancelado</dt>
                <dd className="mt-1 font-semibold text-sf-ink">
                  {formatOrderDate(order.cancelledAt)}
                </dd>
              </div>
            ) : null}
          </dl>
        </Card>

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Pago</h2>
          {payment ? (
            <dl className="grid gap-3 text-sm md:grid-cols-2">
              <div>
                <dt className="text-sf-muted">Estado del pago</dt>
                <dd className="mt-1 text-lg font-semibold text-sf-ink">
                  {paymentStatusLabel(payment.status)}
                </dd>
              </div>
              <div>
                <dt className="text-sf-muted">Método</dt>
                <dd className="mt-1 font-semibold text-sf-ink">
                  {paymentMethodLabel(payment.paymentMethod)}
                </dd>
              </div>
              <div>
                <dt className="text-sf-muted">Monto</dt>
                <dd className="mt-1 font-semibold text-sf-ink">
                  {formatMoney(payment.amount)}
                </dd>
              </div>
              {refundLabel ? (
                <div className="md:col-span-2">
                  <dt className="text-sf-muted">Reembolso</dt>
                  <dd className="mt-1 font-semibold text-sf-ink">
                    {refundLabel}
                  </dd>
                </div>
              ) : null}
            </dl>
          ) : (
            <p className="text-sm text-sf-muted">
              No hay información de pago disponible para este pedido.
            </p>
          )}
        </Card>

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Entrega</h2>
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

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Productos</h2>
          <ul className="grid gap-3">
            {order.items.map((item) => (
              <li
                key={item.id}
                className="flex items-start justify-between gap-4 border-b border-sf-border pb-3 last:border-0 last:pb-0"
              >
                <div>
                  <p className="font-semibold text-sf-ink">{item.productName}</p>
                  <p className="mt-1 text-sm text-sf-muted">
                    {item.quantity} × {formatMoney(item.unitPrice)}
                  </p>
                </div>
                <p className="font-semibold text-sf-ink">
                  {formatMoney(item.subtotal)}
                </p>
              </li>
            ))}
          </ul>
        </Card>

        <CancelOrderPanel order={order} />

        <div className="flex flex-col gap-2 sm:flex-row">
          <Link href="/orders" className={buttonClassName("primary")}>
            Volver a mis pedidos
          </Link>
          <Link href="/catalog" className={buttonClassName("secondary")}>
            Seguir comprando
          </Link>
        </div>
      </div>
    </Container>
  );
}
