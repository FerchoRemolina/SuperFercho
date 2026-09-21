import { request } from "@/shared/api/client";
import type { Money } from "@/shared/money/money";

export type FavoriteProductStatus = "ACTIVE" | "INACTIVE";

/** Mirrors FavoriteRestResponse for POST /api/v1/favorites/{productId}. */
export type Favorite = {
  id: string;
  productId: string;
  createdAt: string;
};

/** Mirrors FavoriteProductRestResponse. `available` is catalog sellable, not stock. */
export type FavoriteProduct = {
  id: string;
  name: string;
  brand: string | null;
  price: Money;
  imageUrl: string | null;
  categoryId: string;
  status: FavoriteProductStatus;
  available: boolean;
};

/** Mirrors FavoriteItemRestResponse. `product` is null when Catalog no longer has the id. */
export type FavoriteItem = {
  productId: string;
  createdAt: string;
  product: FavoriteProduct | null;
};

/** Mirrors FavoriteListRestResponse for GET /api/v1/favorites. */
export type FavoriteList = {
  items: FavoriteItem[];
};

export function favoritesKeys() {
  return {
    root: () => ["favorites"] as const,
  };
}

export async function getFavorites(): Promise<FavoriteList> {
  return request<FavoriteList>("/favorites");
}

export async function addFavorite(productId: string): Promise<Favorite> {
  return request<Favorite>(`/favorites/${encodeURIComponent(productId)}`, {
    method: "POST",
  });
}

export async function removeFavorite(productId: string): Promise<void> {
  await request<void>(`/favorites/${encodeURIComponent(productId)}`, {
    method: "DELETE",
  });
}

export function isProductInFavorites(
  items: FavoriteItem[] | undefined,
  productId: string,
): boolean {
  if (!items) {
    return false;
  }
  return items.some((item) => item.productId === productId);
}

/** Purchase from a favorite uses sellable (`available`), never catalog stock. */
export function isFavoriteProductPurchasable(product: FavoriteProduct | null): boolean {
  return product !== null && product.available && product.status !== "INACTIVE";
}

export function isFavoriteProductUnavailable(product: FavoriteProduct | null): boolean {
  return !isFavoriteProductPurchasable(product);
}
