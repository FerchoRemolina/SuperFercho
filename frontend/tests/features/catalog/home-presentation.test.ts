import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("home customer presentation", () => {
  it("uses a compact HomeProductCard without cart quantity or list controls", () => {
    const card = source("features/catalog/components/home-product-card.tsx");
    const preview = source(
      "features/catalog/components/home-product-preview.tsx",
    );
    const page = source("app/page.tsx");

    expect(preview).toContain("HomeProductCard");
    expect(preview).not.toContain('from "@/features/catalog/components/product-card"');
    expect(preview).not.toContain("<ProductCard");
    expect(preview).not.toContain("Ver catálogo");

    expect(card).toContain("Ver producto");
    expect(card).not.toContain("AddToCartButton");
    expect(card).not.toContain("AddToListControl");
    expect(card).not.toContain("ProductQuantitySelector");
    expect(card).toContain('session?.role === "CUSTOMER"');

    expect(page).toContain("Ver productos");
    expect(page).toContain('href="/catalog"');
    expect(page).toContain("Ver catálogo →");
    expect(page).not.toContain("quickAccess");
    expect(page).not.toContain("Explora los productos del súper");
    expect(page).toContain("brandingAssets.mark");
    expect(page).toContain("Hablar con Fercho");
  });

  it("keeps catalog ProductCard actions while using compact presentation", () => {
    const productCard = source("features/catalog/components/product-card.tsx");
    expect(productCard).toContain("AddToCartButton");
    expect(productCard).toContain("AddToListControl");
    expect(productCard).toContain("FavoriteToggle");
    expect(productCard).toContain('session?.role === "CUSTOMER"');
    expect(productCard).toContain("line-clamp-2");
    expect(productCard).toContain("Disponible");
    expect(productCard).toContain("Agotado");
    expect(productCard).not.toContain("productStockLabel");
    expect(productCard).not.toContain("product.description");
  });

  it("renders branding SVG assets reserved by the design system", () => {
    expect(
      existsSync(
        join(frontendRoot, "public/images/branding/superfercho-logo.svg"),
      ),
    ).toBe(true);
    expect(
      existsSync(
        join(frontendRoot, "public/images/branding/superfercho-mark.svg"),
      ),
    ).toBe(true);
    expect(
      existsSync(
        join(frontendRoot, "public/images/branding/superfercho-logo-dark.svg"),
      ),
    ).toBe(true);

    const logo = source("shared/ui/brand-logo.tsx");
    expect(logo).toContain("brandingAssets.logo");
    expect(logo).toContain("<img");
  });

  it("uses a branded empty state for ProductImage", () => {
    const image = source("features/catalog/components/product-image.tsx");
    expect(image).toContain("brandingAssets.mark");
    expect(image).toContain("Sin imagen");
  });
});
