import Link from "next/link";
import { AddToCartButton } from "@/features/cart/components/add-to-cart-button";
import { ProductImage } from "@/features/catalog/components/product-image";
import {
  isFavoriteProductPurchasable,
  type FavoriteItem,
} from "@/features/favorites/api";
import { FavoriteToggle } from "@/features/favorites/components/favorite-toggle";
import { formatMoney } from "@/shared/money/money";
import { Badge } from "@/shared/ui/badge";
import { Card } from "@/shared/ui/card";

export function FavoriteItemCard({ item }: { item: FavoriteItem }) {
  const product = item.product;
  const purchasable = isFavoriteProductPurchasable(product);

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
          <FavoriteToggle productId={item.productId} />
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
        <Badge tone={purchasable ? "primary" : "danger"} className="w-fit">
          {purchasable ? "Disponible" : "No disponible"}
        </Badge>
        {!purchasable ? (
          <p className="text-sm text-sf-muted">
            Este producto no está a la venta ahora. Puedes mantenerlo o quitarlo de
            favoritos.
          </p>
        ) : null}
        <div className="mt-auto grid gap-3 md:grid-cols-2">
          <FavoriteToggle productId={item.productId} />
          {purchasable ? (
            <AddToCartButton productId={product.id} available={true} />
          ) : null}
        </div>
      </div>
    </Card>
  );
}
