import { Suspense } from "react";
import { AdminKnowledgePageContent } from "@/features/admin/components/admin-knowledge-page";
import { Container } from "@/shared/ui/container";
import { Skeleton } from "@/shared/ui/skeleton";

export default function AdminKnowledgePage() {
  return (
    <Suspense
      fallback={
        <Container as="main" className="py-10 md:py-16">
          <Skeleton className="h-10 w-48" />
          <Skeleton className="mt-8 h-11 w-full" />
          <Skeleton className="mt-8 h-40 w-full" />
        </Container>
      }
    >
      <AdminKnowledgePageContent />
    </Suspense>
  );
}
