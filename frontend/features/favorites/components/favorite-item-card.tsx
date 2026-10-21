import type { ReactElement } from "react";
import Link from "next/link";
import { AddToCartButton } from "@/features/cart/components/add-to-cart-button";
import { ProductImage } from "@/features/catalog/components/product-image";
import {
  canOfferAddToCart,
  productStockLabel,
} from "@/features/catalog/quantity";
import {
  isFavoriteProductPurchasable,
  type FavoriteItem,
} from "@/features/favorites/api";
import { FavoriteToggle } from "@/features/favorites/components/favorite-toggle";
import { formatMoney } from "@/shared/money/money";
import { Badge } from "@/shared/ui/badge";

function availabilityBadge(
  purchasable: boolean,
  outOfStock: boolean,
): ReactElement {
  if (!purchasable) {
    return (
      <Badge tone="danger" className="rounded-full px-2 py-0.5 text-xs">
        No disponible
      </Badge>
    );
  }
  if (outOfStock) {
    return (
      <Badge tone="danger" className="rounded-full px-2 py-0.5 text-xs">
        Agotado
      </Badge>
    );
  }
  return (
    <Badge tone="primary" className="rounded-full px-2 py-0.5 text-xs">
      Disponible
    </Badge>
  );
}

export function FavoriteItemCard({ item }: { item: FavoriteItem }) {
  const product = item.product;
  const purchasable = isFavoriteProductPurchasable(product);
  const stock = product?.stock;
  const offerAddToCart = canOfferAddToCart(purchasable, stock);
  const stockKnown = stock !== undefined;
  const outOfStock = stockKnown && stock <= 0;
  const stockLabel = stockKnown ? productStockLabel(stock) : null;
  const showStockDetail = purchasable && !outOfStock && stockKnown && stock > 0;

  const name = product?.name ?? "Producto no disponible";

  return (
    <li
      className={
        "relative flex h-full flex-col rounded-2xl border border-sf-border bg-sf-surface p-3" +
        " shadow-[0_1px_2px_rgba(23,33,27,0.06)] transition-colors duration-200 hover:border-sf-primary/25"
      }
    >
      <div className="flex items-center justify-between gap-2">
        {product
          ? availabilityBadge(purchasable, outOfStock)
          : (
              <Badge tone="danger" className="rounded-full px-2 py-0.5 text-xs">
                No disponible
              </Badge>
            )}
        <FavoriteToggle productId={item.productId} className="-mr-1.5" />
      </div>

      <div className="mt-2 flex items-center gap-3">
        <div className="w-20 shrink-0">
          {product && purchasable ? (
            <Link href={`/products/${product.id}`} aria-label={name}>
              <ProductImage
                src={product.imageUrl}
                alt=""
                className="rounded-lg"
              />
            </Link>
          ) : (
            <ProductImage
              src={product?.imageUrl ?? null}
              alt=""
              className="rounded-lg"
            />
          )}
        </div>
        <div className="min-w-0 flex-1">
          {product ? (
            <>
              {product.brand ? (
                <p className="truncate text-[11px] font-semibold uppercase tracking-[0.06em] text-sf-muted">
                  {product.brand}
                </p>
              ) : null}
              {purchasable ? (
                <Link
                  href={`/products/${product.id}`}
                  className="line-clamp-2 text-sm font-semibold leading-snug text-sf-ink transition-colors hover:text-sf-primary"
                >
                  {product.name}
                </Link>
              ) : (
                <p className="line-clamp-2 text-sm font-semibold leading-snug text-sf-ink">
                  {product.name}
                </p>
              )}
              <p className="mt-0.5 text-base font-bold tabular-nums text-sf-ink">
                {formatMoney(product.price)}
              </p>
              {showStockDetail && stockLabel ? (
                <p className="mt-0.5 text-xs text-sf-muted">{stockLabel}</p>
              ) : null}
            </>
          ) : (
            <p className="text-sm font-semibold leading-snug text-sf-ink">
              Producto no disponible
            </p>
          )}
        </div>
      </div>

      {product ? (
        <>
          {!purchasable ? (
            <p className="mt-2 text-xs text-sf-muted">
              Este producto no está a la venta ahora. Puedes mantenerlo o
              quitarlo de favoritos.
            </p>
          ) : null}
          {purchasable && outOfStock ? (
            <p className="mt-2 text-xs text-sf-muted">
              Este producto está agotado por ahora. Puedes mantenerlo en
              favoritos.
            </p>
          ) : null}
          {offerAddToCart ? (
            <div className="mt-auto pt-2.5">
              <AddToCartButton
                productId={product.id}
                available={true}
                stock={stock}
                quantitySelector={false}
              />
            </div>
          ) : null}
        </>
      ) : (
        <p className="mt-2 text-xs text-sf-muted">
          Este producto ya no está en el catálogo. Puedes quitarlo de tus
          favoritos con el corazón.
        </p>
      )}
    </li>
  );
}
