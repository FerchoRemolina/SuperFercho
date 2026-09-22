import type { ProductStatus } from "@/features/catalog/api";
import { productStatusLabel } from "@/features/admin/presentation";
import { Badge } from "@/shared/ui/badge";

export function ProductStatusBadge({ status }: { status: ProductStatus }) {
  return (
    <Badge tone={status === "ACTIVE" ? "primary" : "neutral"}>
      {productStatusLabel(status)}
    </Badge>
  );
}
