"use client";

import Link from "next/link";
import { AddToCartButton } from "@/features/cart/components/add-to-cart-button";
import { isProductAvailable } from "@/features/catalog/api";
import { FavoriteToggle } from "@/features/favorites/components/favorite-toggle";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import { ProductImage } from "@/features/catalog/components/product-image";
import { useCategoryQuery, useProductQuery } from "@/features/catalog/hooks";
import { AddToListControl } from "@/features/lists/components/add-to-list-control";
import { isApiError } from "@/shared/errors/api-problem";
import { formatMoney } from "@/shared/money/money";
import { useSession } from "@/shared/session/session-provider";
import { Badge } from "@/shared/ui/badge";
import { buttonClassName } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

function detailAvailabilityLabel(available: boolean): string {
  return available ? "Disponible" : "Agotado";
}

function brandsMatchName(brand: string, name: string): boolean {
  return brand.trim().toLocaleLowerCase("es") === name.trim().toLocaleLowerCase("es");
}

export function ProductDetailContent({ productId }: { productId: string }) {
  const { session } = useSession();
  const productQuery = useProductQuery(productId);
  const categoryQuery = useCategoryQuery(productQuery.data?.categoryId ?? "");

  if (productQuery.isPending) {
    return (
      <Container as="main" className="py-8 md:py-16">
        <Skeleton className="mb-4 h-5 w-28" />
        <div className="grid gap-6 md:grid-cols-2 md:gap-8">
          <Skeleton className="aspect-square w-full" />
          <div>
            <Skeleton className="h-8 w-2/3" />
            <Skeleton className="mt-4 h-5 w-1/3" />
            <Skeleton className="mt-6 h-8 w-40" />
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
      <Container as="main" className="py-8 md:py-16">
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
  const showFavorite = session?.role === "CUSTOMER";
  const showBrand =
    Boolean(product.brand?.trim()) &&
    !brandsMatchName(product.brand ?? "", product.name);

  return (
    <Container as="main" className="py-8 md:py-16">
      <Link
        href="/catalog"
        className="mb-4 inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:text-sf-primary md:mb-6"
      >
        ← Catálogo
      </Link>

      <div className="grid gap-6 md:grid-cols-2 md:items-start md:gap-8">
        <ProductImage src={product.imageUrl} alt={product.name}>
          {showFavorite ? (
            <FavoriteToggle
              productId={product.id}
              className="absolute right-2 top-2 z-10"
            />
          ) : null}
        </ProductImage>

        <div className="min-w-0">
          {category ? (
            <Link
              href={`/categories/${category.id}`}
              className="text-sm font-semibold text-sf-primary"
            >
              {category.name}
            </Link>
          ) : null}
          {showBrand ? (
            <p className="mt-1.5 text-sm text-sf-muted">{product.brand}</p>
          ) : null}
          <h1 className="mt-2 break-words text-[1.75rem] font-bold leading-tight tracking-tight text-sf-ink md:text-[2.75rem]">
            {product.name}
          </h1>
          <p className="mt-3 text-xl font-bold text-sf-ink md:mt-4 md:text-2xl">
            {formatMoney(product.price)}
          </p>
          <Badge tone={available ? "primary" : "danger"} className="mt-3 w-fit md:mt-4">
            {detailAvailabilityLabel(available)}
          </Badge>

          <div className="mt-5 grid gap-2 md:mt-6 md:gap-2.5">
            <AddToCartButton
              productId={product.id}
              available={available}
              stock={product.stock}
              className={cx(
                "!gap-2",
                "[&_.flex]:w-full [&_.flex]:max-w-none [&_.flex]:justify-between",
                "md:[&_.flex]:w-auto md:[&_.flex]:justify-start md:[&_.flex]:gap-1.5",
              )}
            />
            <AddToListControl
              productId={product.id}
              className={cx(
                "border-transparent bg-transparent text-sf-primary shadow-none hover:bg-sf-bg",
                "[&_button]:min-h-10 [&_button]:border-transparent [&_button]:bg-transparent [&_button]:px-2 [&_button]:text-sm [&_button]:font-semibold [&_button]:text-sf-primary [&_button]:shadow-none [&_button]:hover:bg-sf-bg",
              )}
            />
            <Link
              href="/catalog"
              className="inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:text-sf-primary hover:underline"
            >
              Seguir viendo productos
            </Link>
          </div>

          {product.description ? (
            <p className="mt-5 text-base leading-relaxed text-sf-muted md:mt-6">
              {product.description}
            </p>
          ) : null}
        </div>
      </div>
    </Container>
  );
}
