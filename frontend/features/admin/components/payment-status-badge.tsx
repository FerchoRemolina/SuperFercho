import type { PaymentStatus } from "@/features/admin/api";
import { paymentStatusTone } from "@/features/admin/presentation";
import { paymentStatusLabel } from "@/features/orders/api";
import { Badge } from "@/shared/ui/badge";

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  return (
    <Badge tone={paymentStatusTone(status)}>{paymentStatusLabel(status)}</Badge>
  );
}
