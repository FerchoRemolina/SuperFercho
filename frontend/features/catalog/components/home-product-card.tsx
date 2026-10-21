"use client";

import Link from "next/link";
import { type Product } from "@/features/catalog/api";
import { ProductImage } from "@/features/catalog/components/product-image";
import { productAvailabilityLabel } from "@/features/catalog/quantity";
import { FavoriteToggle } from "@/features/favorites/components/favorite-toggle";
import { formatMoney } from "@/shared/money/money";
import { useSession } from "@/shared/session/session-provider";
import { Badge } from "@/shared/ui/badge";
import { buttonClassName } from "@/shared/ui/button";
import { cx } from "@/shared/utils/cx";

/** Compact product presentation for Home only. Catalog keeps ProductCard. */
export function HomeProductCard({ product }: { product: Product }) {
  const { session } = useSession();
  const availability = productAvailabilityLabel(product.status, product.stock);
  const showFavorite = session?.role === "CUSTOMER";

  return (
    <article
      className={cx(
        "group sf-lift relative flex h-full flex-col rounded-2xl border border-sf-border bg-sf-surface p-2.5",
        "shadow-[0_1px_2px_rgba(23,33,27,0.06)] hover:border-sf-primary/35 hover:shadow-[0_10px_28px_rgba(23,33,27,0.10)]",
        "[&_img]:transition-transform [&_img]:duration-300 group-hover:[&_img]:scale-[1.04]",
        "motion-reduce:[&_img]:transition-none motion-reduce:hover:shadow-[0_1px_2px_rgba(23,33,27,0.06)]",
      )}
    >
      <ProductImage src={product.imageUrl} alt="" className="rounded-xl">
        <Link
          href={`/products/${product.id}`}
          className="absolute inset-0 z-0 rounded-xl"
          aria-label={product.name}
        />
        <Badge
          tone={availability === "Disponible" ? "primary" : "danger"}
          className="absolute left-2 top-2 z-10 rounded-full px-2 py-0.5 text-[11px] shadow-sm"
        >
          {availability}
        </Badge>
        {showFavorite ? (
          <FavoriteToggle
            productId={product.id}
            className="absolute right-2 top-2 z-10"
          />
        ) : null}
      </ProductImage>

      <div className="flex min-w-0 flex-1 flex-col px-1.5 pt-3">
        {product.brand ? (
          <p className="truncate text-[11px] font-semibold uppercase tracking-[0.08em] text-sf-muted">
            {product.brand}
          </p>
        ) : null}
        <Link href={`/products/${product.id}`} className="group/name mt-0.5 min-w-0">
          <h3 className="line-clamp-2 min-h-[2.5rem] text-[15px] font-semibold leading-snug text-sf-ink transition-colors group-hover/name:text-sf-primary">
            {product.name}
          </h3>
        </Link>
        <p className="mt-auto pt-2.5 text-lg font-bold leading-none tabular-nums text-sf-ink">
          {formatMoney(product.price)}
        </p>
      </div>

      <div className="mt-3 px-1.5 pb-1">
        <Link
          href={`/products/${product.id}`}
          className={cx(buttonClassName("primary"), "w-full gap-2")}
        >
          Ver producto
          <span
            aria-hidden="true"
            className="transition-transform duration-200 group-hover:translate-x-0.5 motion-reduce:transition-none"
          >
            →
          </span>
        </Link>
      </div>
    </article>
  );
}
