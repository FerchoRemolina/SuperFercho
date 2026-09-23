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
import { Card } from "@/shared/ui/card";

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

  if (!product) {
    return (
      <Card className="grid gap-4 p-4 md:grid-cols-[8rem_1fr]">
        <ProductImage src={null} alt="" />
        <div className="flex min-w-0 flex-col gap-3">
          <h2 className="text-lg font-semibold text-sf-ink">Producto no disponible</h2>
          <p className="text-sm text-sf-muted">
            Este producto ya no está en el catálogo. Puedes quitarlo de tus favoritos.
          </p>
          <Badge tone="danger" className="w-fit">
            No disponible
          </Badge>
          <FavoriteToggle productId={item.productId} variant="action" />
        </div>
      </Card>
    );
  }

  return (
    <Card className="grid gap-4 p-4 md:grid-cols-[8rem_1fr]">
      <ProductImage src={product.imageUrl} alt={product.name} />
      <div className="flex min-w-0 flex-col gap-3">
        {product.brand ? <p className="text-sm text-sf-muted">{product.brand}</p> : null}
        <h2 className="text-lg font-semibold text-sf-ink">
          {purchasable ? (
            <Link href={`/products/${product.id}`} className="hover:text-sf-primary">
              {product.name}
            </Link>
          ) : (
            product.name
          )}
        </h2>
        <p className="text-base font-bold text-sf-ink">{formatMoney(product.price)}</p>
        <div className="flex flex-wrap gap-2">
          <Badge tone={purchasable ? "primary" : "danger"} className="w-fit">
            {purchasable ? "Disponible" : "No disponible"}
          </Badge>
          {stockKnown ? (
            <Badge tone={stock > 0 ? "primary" : "danger"} className="w-fit">
              {productStockLabel(stock)}
            </Badge>
          ) : null}
        </div>
        {!purchasable ? (
          <p className="text-sm text-sf-muted">
            Este producto no está a la venta ahora. Puedes mantenerlo o quitarlo de
            favoritos.
          </p>
        ) : null}
        {purchasable && outOfStock ? (
          <p className="text-sm text-sf-muted">
            Este producto está agotado por ahora. Puedes mantenerlo en favoritos.
          </p>
        ) : null}
        <div className="mt-auto grid gap-3 md:grid-cols-2">
          <FavoriteToggle productId={item.productId} variant="action" />
          {offerAddToCart ? (
            <AddToCartButton
              productId={product.id}
              available={true}
              stock={stock}
            />
          ) : null}
        </div>
      </div>
    </Card>
  );
}
