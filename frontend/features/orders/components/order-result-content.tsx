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
import { useProductsQuery } from "@/features/catalog/hooks";
import { ProductImage } from "@/features/catalog/components/product-image";
import { productsById } from "@/features/orders/checkout-prices";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import {
  CashIcon,
  CreditCardIcon,
  MapPinIcon,
  PackageIcon,
} from "@/shared/ui/icons";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

export function OrderResultContent({ orderId }: { orderId: string }) {
  const orderQuery = useOrderQuery(orderId);
  const productsQuery = useProductsQuery();
  const view = orderDetailView(orderQuery);

  if (view.kind === "loading") {
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <Skeleton className="mb-4 h-9 w-32 rounded-full" />
        <Skeleton className="h-9 w-48 md:h-11" />
        <OrderDetailSkeleton />
      </Container>
    );
  }

  if (view.kind === "error") {
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <Link
          href="/orders"
          className={cx(
            "mb-3 inline-flex min-h-9 items-center gap-1.5 rounded-full border border-sf-border bg-sf-surface px-3.5",
            "text-sm font-semibold text-sf-muted transition-colors duration-150",
            "hover:border-sf-primary/40 hover:text-sf-primary md:mb-4",
          )}
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
  const catalog = productsById(productsQuery.data ?? []);

  return (
    <Container as="main" width="wide" className="py-8 md:py-12">
      <Link
        href="/orders"
        className={cx(
          "mb-4 inline-flex min-h-9 items-center gap-1.5 rounded-full border border-sf-border bg-sf-surface px-3.5",
          "text-sm font-semibold text-sf-muted transition-colors duration-150",
          "hover:border-sf-primary/40 hover:text-sf-primary md:mb-6",
        )}
      >
        ← Pedidos
      </Link>

      <Card className="grid gap-4 !p-4 md:gap-5 md:!p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <h1 className="break-words text-[1.5rem] font-bold leading-tight tracking-tight text-sf-ink md:text-[2rem]">
              {order.orderNumber}
            </h1>
            <p className="mt-1 text-sm text-sf-muted">
              {formatOrderDate(order.createdAt)}
            </p>
          </div>
          <OrderStatusBadge status={order.status} showHint />
        </div>
        <div className="flex items-baseline justify-between gap-4 border-t border-sf-border pt-4">
          <p className="text-sm font-semibold text-sf-muted">Total del pedido</p>
          <p className="text-2xl font-bold tabular-nums text-sf-ink md:text-3xl">
            {formatMoney(order.total)}
          </p>
        </div>
      </Card>

      <div className="mt-4 grid gap-4 md:mt-5 md:gap-5 lg:grid-cols-[minmax(0,1fr)_22rem] lg:items-start">
        <div className="grid min-w-0 gap-4 md:gap-5">
          <Card className="grid gap-3 !p-4 md:gap-4 md:!p-5">
            <div className="flex items-center gap-2.5">
              <span
                aria-hidden="true"
                className="flex size-7 shrink-0 items-center justify-center rounded-full bg-sf-primary-soft text-sf-primary"
              >
                <PackageIcon className="size-4" />
              </span>
              <h2 className="text-base font-semibold text-sf-ink md:text-lg">
                Productos
              </h2>
            </div>
            <ul className="grid gap-2.5">
              {order.items.map((item) => {
                const catalogProduct = catalog.get(item.productId);
                return (
                  <li
                    key={item.id}
                    className="grid grid-cols-[4rem_minmax(0,1fr)] items-start gap-3 border-b border-sf-border pb-2.5 last:border-b-0 last:pb-0 sm:grid-cols-[4rem_minmax(0,1fr)_auto] sm:gap-3.5"
                  >
                    <div className="w-14 shrink-0 sm:w-16">
                      <ProductImage
                        src={catalogProduct?.imageUrl ?? null}
                        alt=""
                      />
                    </div>
                    <div className="min-w-0">
                      <p className="line-clamp-2 text-sm font-semibold leading-snug text-sf-ink md:text-[0.95rem]">
                        {item.productName}
                      </p>
                      <p className="mt-1 text-xs text-sf-muted">
                        Cantidad: {item.quantity} ·{" "}
                        {formatMoney(item.unitPrice)} c/u
                      </p>
                    </div>
                    <p className="col-span-2 text-right text-base font-bold tabular-nums text-sf-ink sm:col-span-1 sm:pt-0.5">
                      {formatMoney(item.subtotal)}
                    </p>
                  </li>
                );
              })}
            </ul>
          </Card>

          <Card className="grid gap-3 !p-4 md:!p-5">
            <div className="flex items-center gap-2.5">
              <span
                aria-hidden="true"
                className="flex size-7 shrink-0 items-center justify-center rounded-full bg-sf-primary-soft text-sf-primary"
              >
                <MapPinIcon className="size-4" />
              </span>
              <h2 className="text-base font-semibold text-sf-ink md:text-lg">
                Entrega
              </h2>
            </div>
            <div className="flex items-start gap-3 rounded-xl border border-sf-primary/30 bg-sf-primary/5 p-3.5">
              <span
                aria-hidden="true"
                className="flex size-10 shrink-0 items-center justify-center rounded-full bg-sf-primary-soft"
              >
                <MapPinIcon className="size-5 text-sf-primary" />
              </span>
              <div className="min-w-0">
                <p className="font-semibold text-sf-ink">
                  {address.recipientName}
                </p>
                <p className="mt-0.5 text-sm text-sf-muted">
                  {address.addressLine}
                </p>
                {address.additionalInfo ? (
                  <p className="mt-0.5 text-sm text-sf-muted">
                    {address.additionalInfo}
                  </p>
                ) : null}
                <p className="mt-0.5 text-sm text-sf-muted">
                  {address.city}, {address.department}
                </p>
                <p className="mt-0.5 text-sm tabular-nums text-sf-muted">
                  {address.phone}
                </p>
              </div>
            </div>
          </Card>
        </div>

        <div className="grid min-w-0 gap-4 md:gap-5">
          <Card className="grid gap-3 !p-4 md:gap-3 md:!p-5">
            <h2 className="text-base font-semibold text-sf-ink md:text-lg">
              Resumen
            </h2>
            <dl className="grid gap-2 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-sf-muted">Subtotal</dt>
                <dd className="font-semibold tabular-nums text-sf-ink">
                  {formatMoney(order.subtotal)}
                </dd>
              </div>
              <div className="flex items-baseline justify-between gap-4 border-t border-sf-border pt-3">
                <dt className="text-base font-semibold text-sf-ink">Total</dt>
                <dd className="text-lg font-bold tabular-nums text-sf-primary">
                  {formatMoney(order.total)}
                </dd>
              </div>
            </dl>

            {payment ? (
              <div className="grid gap-1.5 border-t border-sf-border pt-3 text-sm">
                <p className="text-xs font-medium uppercase tracking-[0.06em] text-sf-muted">
                  Pago
                </p>
                <div className="flex items-center gap-2.5">
                  <span
                    aria-hidden="true"
                    className="flex size-9 shrink-0 items-center justify-center rounded-full bg-sf-bg text-sf-muted"
                  >
                    {(() => {
                      const PaymentIcon =
                        payment.paymentMethod === "SIMULATED_CARD"
                          ? CreditCardIcon
                          : CashIcon;
                      return <PaymentIcon className="size-4.5" />;
                    })()}
                  </span>
                  <p className="min-w-0 text-sf-ink">
                    <span className="block font-semibold">
                      {paymentMethodLabel(payment.paymentMethod)}
                    </span>
                    <span className="block text-sm text-sf-muted">
                      {paymentStatusLabel(payment.status)} ·{" "}
                      {formatMoney(payment.amount)}
                    </span>
                  </p>
                </div>
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
              className="inline-flex min-h-11 items-center justify-center text-sm font-semibold text-sf-muted transition-colors hover:text-sf-primary hover:underline"
            >
              Seguir comprando
            </Link>
          </div>
        </div>
      </div>
    </Container>
  );
}

function OrderDetailSkeleton() {
  return (
    <div className="mt-6 grid gap-4 md:mt-8" aria-hidden="true">
      <Card className="grid gap-3 !p-4 md:!p-6">
        <div className="flex items-start justify-between gap-3">
          <div>
            <Skeleton className="h-8 w-44" />
            <Skeleton className="mt-2 h-4 w-36" />
          </div>
          <Skeleton className="h-7 w-24 rounded-lg" />
        </div>
        <Skeleton className="h-8 w-full" />
      </Card>
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="grid gap-4">
          <Card className="grid gap-3 !p-4 md:!p-5">
            <Skeleton className="h-5 w-24" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </Card>
          <Card className="grid gap-2 !p-4 md:!p-5">
            <Skeleton className="h-5 w-20" />
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-4 w-56" />
          </Card>
        </div>
        <Card className="grid gap-3 !p-4 md:!p-5">
          <Skeleton className="h-5 w-28" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="h-11 w-full" />
        </Card>
      </div>
    </div>
  );
}
