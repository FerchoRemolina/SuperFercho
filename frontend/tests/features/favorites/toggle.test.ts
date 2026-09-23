import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { favoritesKeys } from "@/features/favorites/api";
import {
  canShowFavoriteToggle,
  favoriteHeartFill,
  favoriteToggleLabel,
  favoriteToggleShowsHeart,
} from "@/features/favorites/presentation";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("favorite toggle presentation", () => {
  it("renders an empty heart and add label when the product is not a favorite", () => {
    expect(favoriteHeartFill(false)).toBe("none");
    expect(favoriteToggleLabel(false)).toBe("Añadir a favoritos");
  });

  it("renders a filled heart and remove label when the product is a favorite", () => {
    expect(favoriteHeartFill(true)).toBe("currentColor");
    expect(favoriteToggleLabel(true)).toBe("Quitar de favoritos");
  });

  it("hides the control for ADMIN and keeps it for guests and CUSTOMER", () => {
    expect(canShowFavoriteToggle("ADMIN")).toBe(false);
    expect(canShowFavoriteToggle("CUSTOMER")).toBe(true);
    expect(canShowFavoriteToggle(undefined)).toBe(true);
    expect(canShowFavoriteToggle(null)).toBe(true);
  });

  it("uses a textual action on /favorites and a heart everywhere else", () => {
    expect(favoriteToggleShowsHeart("icon")).toBe(true);
    expect(favoriteToggleShowsHeart("action")).toBe(false);
  });
});

describe("favorite toggle composition", () => {
  it("keeps ProductCard and ProductDetail on the same FavoriteToggle without local mutations", () => {
    const productCard = source("features/catalog/components/product-card.tsx");
    const productDetail = source(
      "features/catalog/components/product-detail-content.tsx",
    );
    const toggle = source("features/favorites/components/favorite-toggle.tsx");
    const hooks = source("features/favorites/hooks.ts");

    expect(productCard).toContain('from "@/features/favorites/components/favorite-toggle"');
    expect(productDetail).toContain(
      'from "@/features/favorites/components/favorite-toggle"',
    );
    expect(productCard).toContain("<FavoriteToggle");
    expect(productDetail).toContain("<FavoriteToggle");
    expect(productCard).not.toContain("useAddFavoriteMutation");
    expect(productCard).not.toContain("useRemoveFavoriteMutation");
    expect(productDetail).not.toContain("useAddFavoriteMutation");
    expect(productDetail).not.toContain("useRemoveFavoriteMutation");

    expect(toggle).toContain("useAddFavoriteMutation");
    expect(toggle).toContain("useRemoveFavoriteMutation");
    expect(toggle).toContain("addMutation.mutateAsync");
    expect(toggle).toContain("removeMutation.mutateAsync");
    expect(hooks).toContain("invalidateQueries");
    expect(toggle).toContain("favoriteHeartFill(favorited)");
    expect(toggle).toContain("favoriteToggleLabel(favorited)");
    expect(toggle).toContain("aria-label={label}");
  });

  it("places the heart over the product image on catalog and detail only", () => {
    const productCard = source("features/catalog/components/product-card.tsx");
    const productDetail = source(
      "features/catalog/components/product-detail-content.tsx",
    );
    const favoriteItem = source(
      "features/favorites/components/favorite-item-card.tsx",
    );
    const image = source("features/catalog/components/product-image.tsx");
    expect(productCard).toContain("<ProductImage");
    expect(productCard).toContain("<FavoriteToggle");
    expect(productCard).toContain('className="absolute right-2 top-2 z-10"');
    expect(productCard).not.toContain('variant="action"');
    expect(productDetail).toContain('className="absolute right-2 top-2 z-10"');
    expect(productDetail).not.toContain('variant="action"');
    expect(favoriteItem).toContain('variant="action"');
    expect(favoriteItem).not.toContain('className="absolute right-2 top-2 z-10"');
    expect(favoriteItem).not.toContain("HeartIcon");
    expect(productCard).not.toContain("useFavoritesQuery");
    expect(image).toContain("relative aspect-square");
    expect(image).toContain("{children}");
  });

  it("preserves the existing query key used after mutations", () => {
    expect(favoritesKeys().root()).toEqual(["favorites"]);
  });

  it("hides the cart CTA when catalog stock is 0 without conflating available", () => {
    const favoriteItem = source(
      "features/favorites/components/favorite-item-card.tsx",
    );
    const page = source(
      "features/favorites/components/favorites-page-content.tsx",
    );

    expect(page).toContain("useProductsQuery");
    expect(page).toContain("catalogProduct={productsById.get(item.productId)}");
    expect(favoriteItem).toContain("canOfferAddToCart");
    expect(favoriteItem).toContain("isFavoriteProductPurchasable");
    expect(favoriteItem).toContain("productStockLabel");
    expect(favoriteItem).toContain("agotado");
    expect(favoriteItem).toContain("offerAddToCart");
    expect(favoriteItem).toContain("Disponible");
    expect(favoriteItem).toContain("No disponible");
    expect(favoriteItem).not.toContain("available={stock");
    expect(favoriteItem).not.toContain("available={!outOfStock");
  });
});
