"use client";

import { useState } from "react";
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
  const [listMenuOpen, setListMenuOpen] = useState(false);
  const available = isProductAvailable(product);
  const availability = productAvailabilityLabel(product.status, product.stock);
  const showFavorite = session?.role === "CUSTOMER";

  return (
    <article
      className={cx(
        "group sf-lift relative flex h-full flex-col rounded-2xl border border-sf-border bg-sf-surface p-2.5",
        "shadow-[0_1px_2px_rgba(23,33,27,0.06)] hover:border-sf-primary/35 hover:shadow-[0_10px_28px_rgba(23,33,27,0.10)]",
        "[&_img]:transition-transform [&_img]:duration-300 group-hover:[&_img]:scale-[1.04]",
        "motion-reduce:[&_img]:transition-none motion-reduce:hover:shadow-[0_1px_2px_rgba(23,33,27,0.06)]",
        // Con el menú de listas abierto, la card queda por encima de sus vecinas.
        listMenuOpen && "z-30",
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

      <div className="flex min-w-0 flex-1 flex-col px-1.5 pt-2.5">
        {product.brand ? (
          <p className="truncate text-[11px] font-semibold uppercase tracking-[0.08em] text-sf-muted">
            {product.brand}
          </p>
        ) : null}
        <Link href={`/products/${product.id}`} className="group/name mt-0.5 min-w-0">
          {/* min-h fijo: nombres de 1 o 2 líneas alinean el precio en toda la fila */}
          <h3 className="line-clamp-2 min-h-[2.5rem] text-[15px] font-semibold leading-snug text-sf-ink transition-colors group-hover/name:text-sf-primary">
            {product.name}
          </h3>
        </Link>
        <p className="mt-auto pt-2 text-lg font-bold leading-none tabular-nums text-sf-ink">
          {formatMoney(product.price)}
        </p>
      </div>

      {available ? (
        <div className="mt-2.5 flex flex-col gap-1.5 border-t border-sf-border/70 px-1.5 pb-1 pt-2.5">
          <AddToCartButton
            productId={product.id}
            available={available}
            stock={product.stock}
            className="gap-2"
          />
          <AddToListControl
            productId={product.id}
            onOpenChange={setListMenuOpen}
            className={cx(
              "border-transparent bg-transparent text-sf-primary shadow-none hover:bg-sf-bg",
              "[&_button]:min-h-10 [&_button]:justify-center [&_button]:rounded-lg [&_button]:border-transparent [&_button]:bg-transparent [&_button]:px-2 [&_button]:text-[13px] [&_button]:font-semibold [&_button]:text-sf-primary [&_button]:shadow-none [&_button]:hover:bg-sf-bg",
            )}
          />
        </div>
      ) : null}
    </article>
  );
}
