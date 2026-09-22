import { AdminKnowledgeDocumentDetailPage } from "@/features/admin/components/admin-knowledge-document-detail-page";

export default async function AdminKnowledgeDocumentPage({
  params,
}: {
  params: Promise<{ documentId: string }>;
}) {
  const { documentId } = await params;

  return (
    <AdminKnowledgeDocumentDetailPage
      key={documentId}
      documentId={documentId}
    />
  );
}
