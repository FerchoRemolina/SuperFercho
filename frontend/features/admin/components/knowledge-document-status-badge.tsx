import type { DocumentStatus } from "@/features/admin/api";
import {
  documentStatusLabel,
  documentStatusTone,
} from "@/features/admin/presentation";
import { Badge } from "@/shared/ui/badge";

export function KnowledgeDocumentStatusBadge({
  status,
}: {
  status: DocumentStatus;
}) {
  return (
    <Badge tone={documentStatusTone(status)}>
      {documentStatusLabel(status)}
    </Badge>
  );
}
