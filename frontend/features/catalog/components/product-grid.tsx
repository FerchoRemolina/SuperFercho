import { ProductCard } from "@/features/catalog/components/product-card";
import type { Product } from "@/features/catalog/api";
import { Card } from "@/shared/ui/card";
import { Skeleton } from "@/shared/ui/skeleton";

export function ProductGrid({ products }: { products: Product[] }) {
  return (
    <ul className="grid grid-cols-2 gap-4 md:grid-cols-4 md:gap-6">
      {products.map((product) => (
        <li key={product.id}>
          <ProductCard product={product} />
        </li>
      ))}
    </ul>
  );
}

export function ProductGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <ul className="grid grid-cols-2 gap-4 md:grid-cols-4 md:gap-6" aria-hidden="true">
      {Array.from({ length: count }, (_, index) => (
        <li key={index}>
          <Card className="p-4">
            <Skeleton className="aspect-square w-full" />
            <Skeleton className="mt-4 h-4 w-3/4" />
            <Skeleton className="mt-2 h-3 w-1/2" />
            <Skeleton className="mt-4 h-5 w-1/3" />
          </Card>
        </li>
      ))}
    </ul>
  );
}
