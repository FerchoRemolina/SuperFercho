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

  it("uses PageHeader navigation and keeps catalog as the back target", () => {
    const detail = source(
      "features/catalog/components/product-detail-content.tsx",
    );
    expect(detail).toContain("<PageHeader");
    expect(detail).toContain('back={{ href: "/catalog", label: "← Catálogo" }}');
    expect(detail).toContain("title={product.name}");
    expect(detail).not.toContain("Seguir viendo productos");
  });

  it("uses short availability labels and action visual hierarchy", () => {
    const detail = source(
      "features/catalog/components/product-detail-content.tsx",
    );
    expect(detail).toContain("productAvailabilityLabel");
    expect(detail).not.toContain("productStockLabel");
    expect(detail).toContain("AddToCartButton");
    expect(detail).toContain("AddToListControl");
    expect(detail).toContain("brandsMatchName");
  });

  it("styles the availability badge as a Customer pill and groups the purchase actions", () => {
    const detail = source(
      "features/catalog/components/product-detail-content.tsx",
    );
    expect(detail).toContain("!rounded-full px-2.5 !py-0.5 !text-[11px]");
    expect(detail).toContain("rounded-2xl border border-sf-border bg-sf-surface p-4");
    expect(detail).toContain("group-hover:[&_img]:scale-[1.04]");
    expect(detail).toContain("motion-reduce:[&_img]:transition-none");
  });
});
