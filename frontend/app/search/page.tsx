import { Suspense } from "react";
import { SearchPageContent } from "@/features/catalog/components/search-page-content";
import { ProductGridSkeleton } from "@/features/catalog/components/product-grid";
import { Container } from "@/shared/ui/container";
import { Skeleton } from "@/shared/ui/skeleton";

export const dynamic = "force-dynamic";

export default function SearchPage() {
  return (
    <Suspense fallback={<SearchPageFallback />}>
      <SearchPageContent />
    </Suspense>
  );
}

function SearchPageFallback() {
  return (
    <Container as="main" className="py-8 md:py-16" aria-hidden="true">
      <Skeleton className="mb-3 h-5 w-28" />
      <Skeleton className="h-9 w-36 md:h-11" />
      <Skeleton className="mt-2 h-4 w-56" />
      <Skeleton className="mt-5 h-11 w-full max-w-xl" />
      <div className="mt-6 md:mt-8">
        <ProductGridSkeleton />
      </div>
    </Container>
  );
}
