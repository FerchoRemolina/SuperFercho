import { Suspense } from "react";
import { SearchPageContent } from "@/features/catalog/components/search-page-content";
import { Container } from "@/shared/ui/container";
import { Skeleton } from "@/shared/ui/skeleton";

export const dynamic = "force-dynamic";

export default function SearchPage() {
  return (
    <Suspense
      fallback={
        <Container as="main" className="py-10 md:py-16">
          <Skeleton className="h-10 w-40" />
          <Skeleton className="mt-8 h-11 w-full max-w-xl" />
        </Container>
      }
    >
      <SearchPageContent />
    </Suspense>
  );
}
