import { Suspense } from "react";
import { AdminProductsPageContent } from "@/features/admin/components/admin-products-page";
import { Container } from "@/shared/ui/container";
import { Skeleton } from "@/shared/ui/skeleton";

export const dynamic = "force-dynamic";

export default function AdminProductsPage() {
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
      <AdminProductsPageContent />
    </Suspense>
  );
}
