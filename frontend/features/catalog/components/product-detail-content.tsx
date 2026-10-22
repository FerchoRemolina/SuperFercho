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
import { PageHeader } from "@/shared/ui/page-header";
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
        <Skeleton className="h-9 w-2/3" />
        <div className="mt-6 grid gap-6 md:grid-cols-2 md:gap-10">
          <Skeleton className="aspect-square w-full rounded-2xl" />
          <div>
            <Skeleton className="h-8 w-28 rounded-full" />
            <Skeleton className="mt-5 h-9 w-40" />
            <div className="mt-6 rounded-2xl border border-sf-border bg-sf-surface p-4">
              <Skeleton className="h-11 w-full max-w-xs" />
              <Skeleton className="mt-3 h-10 w-44" />
            </div>
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
      <PageHeader
        back={{ href: "/catalog", label: "← Catálogo" }}
        title={product.name}
      />

      <div className="mt-6 grid gap-6 md:grid-cols-2 md:items-start md:gap-10">
        <div className="group overflow-hidden rounded-2xl border border-sf-border bg-sf-surface shadow-[0_1px_2px_rgba(23,33,27,0.06)] [&_img]:transition-transform [&_img]:duration-300 group-hover:[&_img]:scale-[1.04] motion-reduce:[&_img]:transition-none">
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
          <div className="mt-4 flex flex-wrap items-center gap-3">
            <p className="text-2xl font-bold tabular-nums text-sf-ink md:text-3xl">
              {formatMoney(product.price)}
            </p>
            <Badge
              tone={availability === "Disponible" ? "primary" : "danger"}
              className="!rounded-full px-2.5 !py-0.5 !text-[11px] shadow-sm"
            >
              {availability}
            </Badge>
          </div>

          <div className="mt-6 rounded-2xl border border-sf-border bg-sf-surface p-4 shadow-[0_1px_2px_rgba(23,33,27,0.06)]">
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
                "mt-1 border-transparent bg-transparent text-sf-primary shadow-none hover:bg-sf-bg",
                "[&_button]:min-h-10 [&_button]:border-transparent [&_button]:bg-transparent [&_button]:px-2 [&_button]:text-sm [&_button]:font-semibold [&_button]:text-sf-primary [&_button]:shadow-none [&_button]:hover:bg-sf-bg",
              )}
            />
          </div>

          {product.description ? (
            <div className="mt-4 rounded-2xl bg-sf-bg p-4 md:p-5">
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
