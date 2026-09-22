import Link from "next/link";
import { AddToCartButton } from "@/features/cart/components/add-to-cart-button";
import { isProductAvailable, type Product } from "@/features/catalog/api";
import { ProductImage } from "@/features/catalog/components/product-image";
import { productStockLabel } from "@/features/catalog/quantity";
import { FavoriteToggle } from "@/features/favorites/components/favorite-toggle";
import { Badge } from "@/shared/ui/badge";
import { formatMoney } from "@/shared/money/money";
import { cx } from "@/shared/utils/cx";

export function ProductCard({ product }: { product: Product }) {
  const available = isProductAvailable(product);

  return (
    <article
      className={cx(
        "flex h-full flex-col rounded-2xl border border-sf-border bg-sf-surface p-4 shadow-[0_1px_2px_rgba(23,33,27,0.06)]",
        "transition-colors hover:border-sf-primary/30",
      )}
    >
      <ProductImage src={product.imageUrl} alt="">
        <Link
          href={`/products/${product.id}`}
          className="absolute inset-0 z-0 rounded-xl"
          aria-label={product.name}
        />
        <FavoriteToggle
          productId={product.id}
          className="absolute right-2 top-2 z-10"
        />
      </ProductImage>
      <Link
        href={`/products/${product.id}`}
        className="group mt-3 flex min-w-0 flex-1 flex-col"
      >
        <div className="flex flex-1 flex-col gap-1">
          {product.brand ? (
            <p className="text-sm text-sf-muted">{product.brand}</p>
          ) : null}
          <h3 className="text-base font-semibold text-sf-ink group-hover:text-sf-primary">
            {product.name}
          </h3>
          {product.description ? (
            <p className="line-clamp-2 text-sm text-sf-muted">{product.description}</p>
          ) : null}
          <p className="mt-auto pt-3 text-base font-bold text-sf-ink">
            {formatMoney(product.price)}
          </p>
          <Badge tone={available ? "primary" : "danger"} className="mt-2 w-fit">
            {productStockLabel(product.stock)}
          </Badge>
        </div>
      </Link>
      <AddToCartButton
        productId={product.id}
        available={available}
        stock={product.stock}
        className="mt-3"
      />
    </article>
  );
}
