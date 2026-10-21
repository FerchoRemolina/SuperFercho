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
import { cx } from "@/shared/utils/cx";

export function OrderCard({ order }: { order: Order }) {
  const paymentSummary = listPaymentSummary(order);
  return (
    <Link href={orderDetailHref(order.id)} className="group block">
      <Card className="sf-lift grid gap-3 !p-4 hover:border-sf-primary/30 md:!p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="min-w-0 truncate text-base font-semibold tracking-tight text-sf-ink md:text-lg">
              {order.orderNumber}
            </p>
            <p className="mt-0.5 text-sm text-sf-muted">
              {formatOrderDate(order.createdAt)}
            </p>
          </div>
          <OrderStatusBadge status={order.status} className="shrink-0" />
        </div>

        <div className="flex flex-wrap items-end justify-between gap-x-4 gap-y-1 border-t border-sf-border pt-3">
          <div>
            <p className="text-xs font-medium uppercase tracking-[0.06em] text-sf-muted">
              Total
            </p>
            <p className="mt-0.5 text-xl font-bold tabular-nums text-sf-ink">
              {formatMoney(order.total)}
            </p>
          </div>
          <div className="flex flex-col items-start gap-0.5 text-xs text-sf-muted sm:items-end">
            <span>{orderItemCountLabel(order)}</span>
            {paymentSummary ? <span>{paymentSummary}</span> : null}
          </div>
        </div>

        <p
          className={cx(
            "text-sm font-semibold text-sf-muted transition-colors",
            "group-hover:text-sf-primary",
          )}
        >
          Ver detalle <span aria-hidden="true">→</span>
        </p>
      </Card>
    </Link>
  );
}
