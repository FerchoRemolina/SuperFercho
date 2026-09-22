import { Suspense } from "react";
import { AdminCategoryDetailPageContent } from "@/features/admin/components/admin-category-detail-page";
import { Container } from "@/shared/ui/container";
import { Skeleton } from "@/shared/ui/skeleton";

export const dynamic = "force-dynamic";

export default async function AdminCategoryDetailPage({
  params,
}: {
  params: Promise<{ categoryId: string }>;
}) {
  const { categoryId } = await params;

  return (
    <Suspense
      fallback={
        <Container as="main" className="py-10 md:py-16">
          <Skeleton className="h-10 w-48" />
          <Skeleton className="mt-8 h-96 w-full" />
        </Container>
      }
    >
      <AdminCategoryDetailPageContent
        key={categoryId}
        categoryId={categoryId}
      />
    </Suspense>
  );
}
