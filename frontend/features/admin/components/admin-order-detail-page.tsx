"use client";

import Link from "next/link";
import { AdminOrderStatusActions } from "@/features/admin/components/admin-order-status-actions";
import { useAdminOrderQuery } from "@/features/admin/hooks";
import {
  adminOrderDetailErrorKind,
  formatAdminInstant,
} from "@/features/admin/presentation";
import {
  paymentMethodLabel,
  paymentStatusLabel,
} from "@/features/orders/api";
import { OrderStatusBadge } from "@/features/orders/components/order-status-badge";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminOrderDetailPageContent({
  orderId,
}: {
  orderId: string;
}) {
  const orderQuery = useAdminOrderQuery(orderId);

  if (orderQuery.isPending) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-40 w-full" />
        <Skeleton className="mt-4 h-32 w-full" />
      </Container>
    );
  }

  if (orderQuery.isError) {
    const kind = adminOrderDetailErrorKind(orderQuery.error);
    const message = isApiError(orderQuery.error)
      ? messageForApiProblem(orderQuery.error.problem)
      : "No se pudo completar la solicitud.";

    if (kind === "order_not_found") {
      return (
        <Container as="main" className="py-10 md:py-16">
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
        </Container>
      );
    }

    return (
      <Container as="main" className="py-10 md:py-16">
        <Alert
          tone="error"
          title={
            kind === "payment_not_found"
              ? "No se pudo cargar el pago"
              : "No se pudo cargar el pedido"
          }
        >
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
      </Container>
    );
  }

  const order = orderQuery.data;
  if (!order) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-40 w-full" />
      </Container>
    );
  }

  const payment = order.payment;
  const address = order.shippingAddress;

  return (
    <Container as="main" className="py-10 md:py-16">
      <Link
        href="/admin/orders"
        className={`${buttonClassName("ghost")} mb-4 px-0`}
      >
        Volver al listado
      </Link>

      <p className="text-sm font-semibold text-sf-muted">Pedido</p>
      <h1 className="mt-1 text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        {order.orderNumber}
      </h1>
      <div className="mt-3">
        <OrderStatusBadge status={order.status} />
      </div>

      <div className="mt-8 grid gap-6">
        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Estado</h2>
          <AdminOrderStatusActions order={order} />
        </Card>

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Cabecera</h2>
          <dl className="grid gap-3 text-sm md:grid-cols-2">
            <div>
              <dt className="text-sf-muted">Número de pedido</dt>
              <dd className="mt-1 text-lg font-semibold text-sf-ink">
                {order.orderNumber}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Cliente</dt>
              <dd className="mt-1 break-all font-semibold text-sf-ink">
                {order.customerId}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Creado</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {formatAdminInstant(order.createdAt)}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Actualizado</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {formatAdminInstant(order.updatedAt)}
              </dd>
            </div>
            {order.confirmedAt ? (
              <div>
                <dt className="text-sf-muted">Confirmado</dt>
                <dd className="mt-1 font-semibold text-sf-ink">
                  {formatAdminInstant(order.confirmedAt)}
                </dd>
              </div>
            ) : null}
            {order.cancelledAt ? (
              <div>
                <dt className="text-sf-muted">Cancelado</dt>
                <dd className="mt-1 font-semibold text-sf-ink">
                  {formatAdminInstant(order.cancelledAt)}
                </dd>
              </div>
            ) : null}
          </dl>
        </Card>

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Resumen</h2>
          <dl className="grid gap-3 text-sm md:grid-cols-2">
            <div>
              <dt className="text-sf-muted">Subtotal</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {formatMoney(order.subtotal)}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Total</dt>
              <dd className="mt-1 text-lg font-bold text-sf-ink">
                {formatMoney(order.total)}
              </dd>
            </div>
          </dl>
        </Card>

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Productos</h2>
          <ul className="grid gap-3">
            {order.items.map((item) => (
              <li
                key={item.id}
                className="grid gap-2 border-b border-sf-border pb-3 last:border-0 last:pb-0 sm:grid-cols-[1fr_auto] sm:items-start sm:justify-between sm:gap-4"
              >
                <div>
                  <p className="font-semibold text-sf-ink">{item.productName}</p>
                  <p className="mt-1 break-all text-sm text-sf-muted">
                    Producto: {item.productId}
                  </p>
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

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">
            Dirección de envío
          </h2>
          <p className="text-sm text-sf-muted">
            Snapshot histórico al momento del pedido.
          </p>
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
          <h2 className="text-xl font-semibold text-sf-ink">Pago</h2>
          {payment ? (
            <dl className="grid gap-3 text-sm md:grid-cols-2">
              <div>
                <dt className="text-sf-muted">Id de pago</dt>
                <dd className="mt-1 break-all font-semibold text-sf-ink">
                  {payment.paymentId}
                </dd>
              </div>
              <div>
                <dt className="text-sf-muted">Monto</dt>
                <dd className="mt-1 font-semibold text-sf-ink">
                  {formatMoney(payment.amount)}
                </dd>
              </div>
              <div>
                <dt className="text-sf-muted">Método</dt>
                <dd className="mt-1 font-semibold text-sf-ink">
                  {paymentMethodLabel(payment.paymentMethod)}
                </dd>
              </div>
              <div>
                <dt className="text-sf-muted">Estado del pago</dt>
                <dd className="mt-1 font-semibold text-sf-ink">
                  {paymentStatusLabel(payment.status)}
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
              {payment.refundedAt ? (
                <div>
                  <dt className="text-sf-muted">Reembolsado</dt>
                  <dd className="mt-1 font-semibold text-sf-ink">
                    {formatAdminInstant(payment.refundedAt)}
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
            </dl>
          ) : (
            <p className="text-sm text-sf-muted">
              No hay información de pago disponible para este pedido.
            </p>
          )}
        </Card>
      </div>
    </Container>
  );
}
