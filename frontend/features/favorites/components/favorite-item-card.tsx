import Link from "next/link";
import { AddToCartButton } from "@/features/cart/components/add-to-cart-button";
import type { Product } from "@/features/catalog/api";
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
import { cx } from "@/shared/utils/cx";

export function FavoriteItemCard({
  item,
  catalogProduct,
}: {
  item: FavoriteItem;
  /** Public catalog row used only for stock; sellability stays on FavoriteProduct.available. */
  catalogProduct?: Product;
}) {
  const product = item.product;
  const purchasable = isFavoriteProductPurchasable(product);
  const stock = catalogProduct?.stock;
  const offerAddToCart = canOfferAddToCart(purchasable, stock);
  const stockKnown = stock !== undefined;
  const outOfStock = stockKnown && stock <= 0;
  const stockLabel = stockKnown ? productStockLabel(stock) : null;
  const showStockDetail = purchasable && !outOfStock && stockKnown && stock > 0;

  if (!product) {
    return (
      <li
        className={cx(
          "grid gap-3 rounded-2xl border border-sf-border bg-sf-surface p-3",
          "md:grid-cols-[5rem_minmax(0,1fr)] md:items-start md:gap-4 md:p-4",
        )}
      >
        <div className="w-[3.75rem] shrink-0 sm:w-[4.5rem] md:w-full">
          <ProductImage src={null} alt="" />
        </div>
        <div className="min-w-0">
          <p className="line-clamp-2 text-base font-semibold text-sf-ink">
            Producto no disponible
          </p>
          <p className="mt-1 text-xs text-sf-muted md:text-sm">
            Este producto ya no está en el catálogo. Puedes quitarlo de tus
            favoritos.
          </p>
          <Badge tone="danger" className="mt-1.5 w-fit">
            No disponible
          </Badge>
          <div className="mt-2.5">
            <FavoriteToggle productId={item.productId} variant="action" />
          </div>
        </div>
      </li>
    );
  }

  return (
    <li
      className={cx(
        "grid gap-3 rounded-2xl border border-sf-border bg-sf-surface p-3",
        "md:grid-cols-[5rem_minmax(0,1fr)] md:items-start md:gap-4 md:p-4",
      )}
    >
      <div className="flex gap-3 md:contents">
        <div className="w-[3.75rem] shrink-0 sm:w-[4.5rem] md:w-full">
          {purchasable ? (
            <Link href={`/products/${product.id}`} aria-label={product.name}>
              <ProductImage src={product.imageUrl} alt="" />
            </Link>
          ) : (
            <ProductImage src={product.imageUrl} alt="" />
          )}
        </div>

        <div className="min-w-0 flex-1">
          {purchasable ? (
            <Link
              href={`/products/${product.id}`}
              className="line-clamp-2 text-base font-semibold text-sf-ink hover:text-sf-primary"
            >
              {product.name}
            </Link>
          ) : (
            <p className="line-clamp-2 text-base font-semibold text-sf-ink">
              {product.name}
            </p>
          )}
          {product.brand ? (
            <p className="mt-0.5 truncate text-sm text-sf-muted">{product.brand}</p>
          ) : null}
          <p className="mt-1 text-base font-bold text-sf-ink">
            {formatMoney(product.price)}
          </p>

          <div className="mt-1.5">
            {!purchasable ? (
              <Badge tone="danger" className="w-fit">
                No disponible
              </Badge>
            ) : outOfStock ? (
              <Badge tone="danger" className="w-fit">
                Agotado
              </Badge>
            ) : (
              <Badge tone="primary" className="w-fit">
                Disponible
              </Badge>
            )}
          </div>
          {showStockDetail && stockLabel ? (
            <p className="mt-1 text-xs text-sf-muted">{stockLabel}</p>
          ) : null}

          {!purchasable ? (
            <p className="mt-1 text-xs text-sf-muted md:text-sm">
              Este producto no está a la venta ahora. Puedes mantenerlo o
              quitarlo de favoritos.
            </p>
          ) : null}
          {purchasable && outOfStock ? (
            <p className="mt-1 text-xs text-sf-muted md:text-sm">
              Este producto está agotado por ahora. Puedes mantenerlo en
              favoritos.
            </p>
          ) : null}

          <div className="mt-2.5 flex flex-col gap-2 sm:flex-row sm:items-end sm:gap-3">
            {offerAddToCart ? (
              <AddToCartButton
                productId={product.id}
                available={true}
                stock={stock}
                className="min-w-0 gap-1.5 sm:max-w-xs"
              />
            ) : null}
            <FavoriteToggle
              productId={item.productId}
              variant="action"
              className="shrink-0 sm:pb-0.5"
            />
          </div>
        </div>
      </div>
    </li>
  );
}
