import type { ProductVariantStatus } from "@/features/admin/api";
import { productVariantStatusLabel } from "@/features/admin/presentation";
import { Badge } from "@/shared/ui/badge";

export function ProductVariantStatusBadge({
  status,
}: {
  status: ProductVariantStatus;
}) {
  return (
    <Badge tone={status === "ACTIVE" ? "primary" : "neutral"}>
      {productVariantStatusLabel(status)}
    </Badge>
  );
}
