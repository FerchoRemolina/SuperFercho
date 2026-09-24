import type { OrderStatus } from "@/features/orders/api";
import { orderStatusLabel } from "@/features/orders/api";
import {
  orderStatusBadgeClassName,
  orderStatusHint,
  orderStatusTone,
} from "@/features/orders/order-views";
import { Badge } from "@/shared/ui/badge";
import { cx } from "@/shared/utils/cx";

export function OrderStatusBadge({
  status,
  showHint = false,
  className,
}: {
  status: OrderStatus;
  showHint?: boolean;
  className?: string;
}) {
  const hint = showHint ? orderStatusHint(status) : null;
  return (
    <span className={cx("inline-flex flex-col items-start gap-1", className)}>
      <Badge
        tone={orderStatusTone(status)}
        className={orderStatusBadgeClassName(status)}
      >
        {orderStatusLabel(status)}
      </Badge>
      {hint ? (
        <span className="max-w-prose text-xs font-medium text-sf-muted md:text-sm">
          {hint}
        </span>
      ) : null}
    </span>
  );
}
