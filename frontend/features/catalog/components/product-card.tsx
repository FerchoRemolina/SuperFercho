"use client";

import Link from "next/link";
import { AddToCartButton } from "@/features/cart/components/add-to-cart-button";
import { isProductAvailable, type Product } from "@/features/catalog/api";
import { ProductImage } from "@/features/catalog/components/product-image";
import { productAvailabilityLabel } from "@/features/catalog/quantity";
import { FavoriteToggle } from "@/features/favorites/components/favorite-toggle";
import { AddToListControl } from "@/features/lists/components/add-to-list-control";
import { formatMoney } from "@/shared/money/money";
import { useSession } from "@/shared/session/session-provider";
import { Badge } from "@/shared/ui/badge";
import { cx } from "@/shared/utils/cx";

export function ProductCard({ product }: { product: Product }) {
  const { session } = useSession();
  const available = isProductAvailable(product);
  const availability = productAvailabilityLabel(product.status, product.stock);
  const showFavorite = session?.role === "CUSTOMER";

  return (
    <article
      className={cx(
        "flex h-full flex-col rounded-2xl border border-sf-border bg-sf-surface p-3 shadow-[0_1px_2px_rgba(23,33,27,0.06)]",
        "transition-colors hover:border-sf-primary/30 md:p-4",
      )}
    >
      <ProductImage src={product.imageUrl} alt="">
        <Link
          href={`/products/${product.id}`}
          className="absolute inset-0 z-0 rounded-xl"
          aria-label={product.name}
        />
        {showFavorite ? (
          <FavoriteToggle
            productId={product.id}
            className="absolute right-2 top-2 z-10"
          />
        ) : null}
      </ProductImage>

      <div className="mt-3 flex min-w-0 flex-1 flex-col gap-1">
        {product.brand ? (
          <p className="truncate text-sm text-sf-muted">{product.brand}</p>
        ) : null}
        <Link href={`/products/${product.id}`} className="group min-w-0">
          <h3 className="line-clamp-2 text-base font-semibold text-sf-ink group-hover:text-sf-primary">
            {product.name}
          </h3>
        </Link>
        <p className="mt-auto pt-2 text-base font-bold text-sf-ink">
          {formatMoney(product.price)}
        </p>
        <Badge
          tone={availability === "Disponible" ? "primary" : "danger"}
          className="mt-2 w-fit"
        >
          {availability}
        </Badge>
      </div>

      <AddToCartButton
        productId={product.id}
        available={available}
        stock={product.stock}
        className={cx(
          "mt-3 !gap-1.5",
          "[&>button]:px-2 [&>button]:text-sm [&>button]:leading-tight [&>button]:whitespace-normal",
        )}
      />
      <AddToListControl
        productId={product.id}
        className={cx(
          "mt-1.5",
          "border-transparent bg-transparent text-sf-primary shadow-none hover:bg-sf-bg",
          "[&_button]:min-h-10 [&_button]:border-transparent [&_button]:bg-transparent [&_button]:px-2 [&_button]:text-sm [&_button]:font-semibold [&_button]:text-sf-primary [&_button]:shadow-none [&_button]:hover:bg-sf-bg",
        )}
      />
    </article>
  );
}
