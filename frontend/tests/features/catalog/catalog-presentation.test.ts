import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("catalog customer presentation", () => {
  it("keeps category chip hrefs while enabling compact horizontal scroll on mobile", () => {
    const chips = source("features/catalog/components/category-chips.tsx");
    expect(chips).toContain('href="/catalog"');
    expect(chips).toContain("`/categories/${category.id}`");
    expect(chips).toContain("overflow-x-auto");
    expect(chips).toContain("md:flex-wrap");
    expect(chips).toContain("max-w-[12rem]");
  });

  it("compacts quantity selector chrome without removing touch targets", () => {
    const selector = source(
      "features/catalog/components/product-quantity-selector.tsx",
    );
    expect(selector).toContain("min-h-11");
    expect(selector).toContain("justify-center");
    expect(selector).toContain("gap-1.5");
  });
});
