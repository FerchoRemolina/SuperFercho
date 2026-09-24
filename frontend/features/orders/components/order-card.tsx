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
      <Card className="grid gap-2.5 !p-4 transition-shadow hover:shadow-[0_8px_24px_rgba(23,33,27,0.08)] md:!p-4">
        <div className="flex items-start justify-between gap-3">
          <p className="min-w-0 text-base font-semibold tracking-tight text-sf-ink md:text-lg">
            {order.orderNumber}
          </p>
          <OrderStatusBadge status={order.status} className="shrink-0" />
        </div>

        <p className="text-lg font-bold text-sf-ink md:text-xl">
          {formatMoney(order.total)}
        </p>

        <p className="text-sm text-sf-muted">{formatOrderDate(order.createdAt)}</p>

        <div className="flex flex-wrap items-center gap-x-3 gap-y-0.5 text-xs text-sf-muted">
          <span>{orderItemCountLabel(order)}</span>
          {paymentSummary ? <span>{paymentSummary}</span> : null}
        </div>

        <p className="text-sm font-semibold text-sf-muted">Ver detalle</p>
      </Card>
    </Link>
  );
}
