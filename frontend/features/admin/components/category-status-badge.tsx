import type { CategoryStatus } from "@/features/catalog/api";
import { categoryStatusLabel } from "@/features/admin/presentation";
import { Badge } from "@/shared/ui/badge";

export function CategoryStatusBadge({ status }: { status: CategoryStatus }) {
  return (
    <Badge tone={status === "ACTIVE" ? "primary" : "neutral"}>
      {categoryStatusLabel(status)}
    </Badge>
  );
}
