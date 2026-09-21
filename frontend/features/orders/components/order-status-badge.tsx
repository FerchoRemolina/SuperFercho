import type { OrderStatus } from "@/features/orders/api";
import { orderStatusLabel } from "@/features/orders/api";
import {
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
      <Badge tone={orderStatusTone(status)}>
        Estado: {orderStatusLabel(status)}
      </Badge>
      {hint ? (
        <span className="text-sm font-medium text-sf-muted">{hint}</span>
      ) : null}
    </span>
  );
}
