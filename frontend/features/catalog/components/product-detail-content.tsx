"use client";

import Link from "next/link";
import { AddToCartButton } from "@/features/cart/components/add-to-cart-button";
import { isProductAvailable } from "@/features/catalog/api";
import { FavoriteToggle } from "@/features/favorites/components/favorite-toggle";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import { ProductImage } from "@/features/catalog/components/product-image";
import { useCategoryQuery, useProductQuery } from "@/features/catalog/hooks";
import { productAvailabilityLabel } from "@/features/catalog/quantity";
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

function brandsMatchName(brand: string, name: string): boolean {
  return brand.trim().toLocaleLowerCase("es") === name.trim().toLocaleLowerCase("es");
}

export function ProductDetailContent({ productId }: { productId: string }) {
  const { session } = useSession();
  const productQuery = useProductQuery(productId);
  const categoryQuery = useCategoryQuery(productQuery.data?.categoryId ?? "");

  if (productQuery.isPending) {
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <Skeleton className="mb-4 h-9 w-32 rounded-full" />
        <div className="grid gap-6 md:grid-cols-2 md:gap-10">
          <Skeleton className="aspect-square w-full rounded-2xl" />
          <div>
            <Skeleton className="h-5 w-24 rounded-full" />
            <Skeleton className="mt-4 h-9 w-2/3" />
            <Skeleton className="mt-5 h-9 w-40" />
            <Skeleton className="mt-6 h-11 w-full max-w-xs" />
            <Skeleton className="mt-4 h-24 w-full" />
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
      <Container as="main" width="wide" className="py-8 md:py-12">
        {notFound ? (
          <EmptyState
            title="No encontramos este producto."
            description="Puede que ya no esté disponible. Vuelve al catálogo para ver otras opciones."
            align="center"
            className="py-14"
            action={
              <Link href="/catalog" className={buttonClassName("primary")}>
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
  const availability = productAvailabilityLabel(product.status, product.stock);
  const category = categoryQuery.data;
  const showFavorite = session?.role === "CUSTOMER";
  const showBrand =
    Boolean(product.brand?.trim()) &&
    !brandsMatchName(product.brand ?? "", product.name);

  return (
    <Container as="main" width="wide" className="py-8 md:py-12">
      <Link
        href="/catalog"
        className={cx(
          "mb-4 inline-flex min-h-9 items-center gap-1.5 rounded-full border border-sf-border bg-sf-surface px-3.5",
          "text-sm font-semibold text-sf-muted transition-colors duration-150",
          "hover:border-sf-primary/40 hover:text-sf-primary md:mb-6",
        )}
      >
        ← Catálogo
      </Link>

      <div className="grid gap-6 md:grid-cols-2 md:items-start md:gap-10">
        <div className="overflow-hidden rounded-2xl border border-sf-border bg-sf-surface shadow-[0_1px_2px_rgba(23,33,27,0.06)]">
          <ProductImage src={product.imageUrl} alt={product.name}>
            {showFavorite ? (
              <FavoriteToggle
                productId={product.id}
                className="absolute right-2 top-2 z-10"
              />
            ) : null}
          </ProductImage>
        </div>

        <div className="min-w-0">
          {category ? (
            <Link
              href={`/categories/${category.id}`}
              className="inline-flex min-h-8 items-center rounded-full bg-sf-primary-soft px-3 text-xs font-semibold text-sf-primary transition-colors hover:bg-sf-primary/20"
            >
              {category.name}
            </Link>
          ) : null}
          {showBrand ? (
            <p className="mt-2.5 text-xs font-medium uppercase tracking-[0.06em] text-sf-muted">
              {product.brand}
            </p>
          ) : null}
          <h1 className="mt-2 break-words text-[1.5rem] font-bold leading-tight tracking-tight text-sf-ink md:text-[2rem]">
            {product.name}
          </h1>
          <div className="mt-4 flex flex-wrap items-center gap-3">
            <p className="text-2xl font-bold tabular-nums text-sf-ink md:text-3xl">
              {formatMoney(product.price)}
            </p>
            <Badge
              tone={availability === "Disponible" ? "primary" : "danger"}
              className="rounded-md px-2 py-0.5 text-xs"
            >
              {availability}
            </Badge>
          </div>

          <div className="mt-6 grid gap-2.5 border-t border-sf-border pt-6 md:gap-3">
            <AddToCartButton
              productId={product.id}
              available={available}
              stock={product.stock}
              className={cx(
                "!gap-2.5",
                "[&>button]:min-h-12 md:[&>button]:min-h-11 md:[&>button]:w-auto md:[&>button]:self-start md:[&>button]:px-8",
                "[&>div.flex]:w-full [&>div.flex]:max-w-none",
                "md:[&>div.flex]:w-auto",
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
              className="inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted transition-colors hover:text-sf-primary hover:underline"
            >
              Seguir viendo productos
            </Link>
          </div>

          {product.description ? (
            <div className="mt-6 rounded-2xl bg-sf-bg p-4 md:p-5">
              <h2 className="text-sm font-semibold text-sf-ink">Descripción</h2>
              <p className="mt-1.5 text-sm leading-relaxed text-sf-muted md:text-base">
                {product.description}
              </p>
            </div>
          ) : null}
        </div>
      </div>
    </Container>
  );
}
