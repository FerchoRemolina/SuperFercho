import type { ProductTypeStatus } from "@/features/admin/api";
import { productTypeStatusLabel } from "@/features/admin/presentation";
import { Badge } from "@/shared/ui/badge";

export function ProductTypeStatusBadge({
  status,
}: {
  status: ProductTypeStatus;
}) {
  return (
    <Badge tone={status === "ACTIVE" ? "primary" : "neutral"}>
      {productTypeStatusLabel(status)}
    </Badge>
  );
}
