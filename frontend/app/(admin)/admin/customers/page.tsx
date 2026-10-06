import { Suspense } from "react";
import { AdminCustomersPageContent } from "@/features/admin/components/admin-customers-page";
import { Skeleton } from "@/shared/ui/skeleton";

export default function AdminCustomersPage() {
  return (
    <Suspense
      fallback={
        <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
          <Skeleton className="h-8 w-64" />
          <Skeleton className="mt-6 h-10 w-full max-w-xl" />
          <Skeleton className="mt-5 h-40 w-full" />
        </main>
      }
    >
      <AdminCustomersPageContent />
    </Suspense>
  );
}
