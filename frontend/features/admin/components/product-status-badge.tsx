import type { ProductStatus } from "@/features/catalog/api";
import {
  productStatusLabel,
  productStatusTone,
} from "@/features/admin/presentation";
import { Badge } from "@/shared/ui/badge";

export function ProductStatusBadge({ status }: { status: ProductStatus }) {
  return (
    <Badge tone={productStatusTone(status)}>{productStatusLabel(status)}</Badge>
  );
}
