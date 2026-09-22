import { Suspense } from "react";
import { AdminOrdersPageContent } from "@/features/admin/components/admin-orders-page";
import { Container } from "@/shared/ui/container";
import { Skeleton } from "@/shared/ui/skeleton";

export default function AdminOrdersPage() {
  return (
    <Suspense
      fallback={
        <Container as="main" className="py-10 md:py-16">
          <Skeleton className="h-10 w-48" />
          <Skeleton className="mt-8 h-11 w-full max-w-sm" />
          <Skeleton className="mt-8 h-40 w-full" />
        </Container>
      }
    >
      <AdminOrdersPageContent />
    </Suspense>
  );
}
