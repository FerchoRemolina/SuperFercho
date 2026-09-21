import Link from "next/link";
import type { Order } from "@/features/orders/api";
import { OrderStatusBadge } from "@/features/orders/components/order-status-badge";
import {
  formatOrderDate,
  listPaymentSummary,
  orderDetailHref,
  orderItemCountLabel,
} from "@/features/orders/order-views";
import { formatMoney } from "@/shared/money/money";
import { Card } from "@/shared/ui/card";

export function OrderCard({ order }: { order: Order }) {
  const paymentSummary = listPaymentSummary(order);
  return (
    <Link href={orderDetailHref(order.id)} className="block">
      <Card className="grid gap-4 transition-shadow hover:shadow-[0_8px_24px_rgba(23,33,27,0.08)]">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-sm text-sf-muted">Número de pedido</p>
            <p className="mt-1 text-lg font-semibold text-sf-ink">
              {order.orderNumber}
            </p>
          </div>
          <OrderStatusBadge status={order.status} />
        </div>
        <dl className="grid gap-3 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-sf-muted">Fecha</dt>
            <dd className="mt-1 font-semibold text-sf-ink">
              {formatOrderDate(order.createdAt)}
            </dd>
          </div>
          <div>
            <dt className="text-sf-muted">Total</dt>
            <dd className="mt-1 font-bold text-sf-ink">
              {formatMoney(order.total)}
            </dd>
          </div>
          <div>
            <dt className="text-sf-muted">Productos</dt>
            <dd className="mt-1 font-semibold text-sf-ink">
              {orderItemCountLabel(order)}
            </dd>
          </div>
          {paymentSummary ? (
            <div>
              <dt className="text-sf-muted">Pago</dt>
              <dd className="mt-1 font-semibold text-sf-ink">{paymentSummary}</dd>
            </div>
          ) : null}
        </dl>
        <p className="text-sm font-semibold text-sf-primary">Ver detalle</p>
      </Card>
    </Link>
  );
}
