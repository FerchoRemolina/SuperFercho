"use client";

import Link from "next/link";
import { AddToCartButton } from "@/features/cart/components/add-to-cart-button";
import { isProductAvailable } from "@/features/catalog/api";
import { FavoriteToggle } from "@/features/favorites/components/favorite-toggle";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import { ProductImage } from "@/features/catalog/components/product-image";
import { productStockLabel } from "@/features/catalog/quantity";
import { useCategoryQuery, useProductQuery } from "@/features/catalog/hooks";
import { AddToListControl } from "@/features/lists/components/add-to-list-control";
import { isApiError } from "@/shared/errors/api-problem";
import { formatMoney } from "@/shared/money/money";
import { Badge } from "@/shared/ui/badge";
import { buttonClassName } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function ProductDetailContent({ productId }: { productId: string }) {
  const productQuery = useProductQuery(productId);
  const categoryQuery = useCategoryQuery(productQuery.data?.categoryId ?? "");

  if (productQuery.isPending) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <div className="grid gap-8 md:grid-cols-2">
          <Skeleton className="aspect-square w-full" />
          <div>
            <Skeleton className="h-8 w-2/3" />
            <Skeleton className="mt-4 h-5 w-1/3" />
            <Skeleton className="mt-8 h-8 w-40" />
            <Skeleton className="mt-6 h-24 w-full" />
          </div>
        </div>
      </Container>
    );
  }

  if (productQuery.isError) {
    const notFound =
      isApiError(productQuery.error) &&
      productQuery.error.problem.code === "PRODUCT_NOT_FOUND";
    return (
      <Container as="main" className="py-10 md:py-16">
        {notFound ? (
          <EmptyState
            title="No encontramos este producto."
            description="Puede que ya no esté disponible. Vuelve al catálogo para ver otras opciones."
            action={
              <Link href="/catalog" className={buttonClassName("secondary")}>
                Ir al catálogo
              </Link>
            }
          />
        ) : (
          <CatalogQueryError
            error={productQuery.error}
            title="No se pudo cargar el producto"
          />
        )}
      </Container>
    );
  }

  const product = productQuery.data;
  if (!product) {
    return null;
  }

  const available = isProductAvailable(product);
  const category = categoryQuery.data;

  return (
    <Container as="main" className="py-10 md:py-16">
      <div className="grid gap-8 md:grid-cols-2 md:items-start">
        <ProductImage src={product.imageUrl} alt={product.name}>
          <FavoriteToggle
            productId={product.id}
            className="absolute right-2 top-2 z-10"
          />
        </ProductImage>
        <div>
          {category ? (
            <Link
              href={`/categories/${category.id}`}
              className="text-sm font-semibold text-sf-primary"
            >
              {category.name}
            </Link>
          ) : null}
          {product.brand ? (
            <p className="mt-2 text-sm text-sf-muted">{product.brand}</p>
          ) : null}
          <h1 className="mt-2 text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            {product.name}
          </h1>
          <p className="mt-4 text-2xl font-bold text-sf-ink">
            {formatMoney(product.price)}
          </p>
          <Badge tone={available ? "primary" : "danger"} className="mt-4">
            {productStockLabel(product.stock)}
          </Badge>
          {product.description ? (
            <p className="mt-6 text-base text-sf-muted">{product.description}</p>
          ) : null}
          <div className="mt-8 grid gap-3">
            <AddToCartButton
              productId={product.id}
              available={available}
              stock={product.stock}
            />
            <AddToListControl productId={product.id} />
            <Link href="/catalog" className={buttonClassName("secondary")}>
              Seguir viendo productos
            </Link>
          </div>
        </div>
      </div>
    </Container>
  );
}
