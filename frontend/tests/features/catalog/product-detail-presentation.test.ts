import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("product detail presentation", () => {
  it("keeps FavoriteToggle overlaid on the image for CUSTOMER only", () => {
    const detail = source(
      "features/catalog/components/product-detail-content.tsx",
    );
    expect(detail).toContain("<ProductImage");
    expect(detail).toContain("<FavoriteToggle");
    expect(detail).toContain('className="absolute right-2 top-2 z-10"');
    expect(detail).toContain('session?.role === "CUSTOMER"');
    expect(detail).not.toContain("canShowFavoriteToggle");
    expect(detail).not.toContain("useAddFavoriteMutation");
  });

  it("uses short availability labels and action visual hierarchy", () => {
    const detail = source(
      "features/catalog/components/product-detail-content.tsx",
    );
    expect(detail).toContain("← Catálogo");
    expect(detail).toContain('href="/catalog"');
    expect(detail).toContain("productAvailabilityLabel");
    expect(detail).not.toContain("productStockLabel");
    expect(detail).toContain("AddToCartButton");
    expect(detail).toContain("AddToListControl");
    expect(detail).toContain("Seguir viendo productos");
    expect(detail).toContain("brandsMatchName");
  });
});
