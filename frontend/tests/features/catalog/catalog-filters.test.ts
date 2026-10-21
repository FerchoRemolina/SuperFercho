import { describe, expect, it } from "vitest";
import type { Product } from "@/features/catalog/api";
import {
  applyProductCatalogFilters,
  collectPresentationOptions,
  collectProductTypeOptions,
  collectProductVariantOptions,
  EMPTY_PRODUCT_CATALOG_FILTERS,
  formatPresentationLabel,
  formatPriceFilterLabel,
  hasActiveProductFilters,
  normalizePriceBounds,
  parsePriceInput,
  priceRangeBounds,
  presentationKeyOf,
  PRODUCT_SORT_OPTIONS,
  sortProducts,
  withProductType,
  withProductVariant,
} from "@/features/catalog/catalog-filters";

function product(overrides: Partial<Product> & { id: string }): Product {
  return {
    categoryId: "cat-bebidas",
    barcode: null,
    name: `Producto ${overrides.id}`,
    brand: null,
    description: null,
    price: { amount: 5000, currency: "COP" },
    stock: 10,
    imageUrl: null,
    status: "ACTIVE",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

// Jerarquía real: Bebidas → Gaseosas (Original, Sin azúcar) / Agua (Natural).
const catalog: Product[] = [
  product({
    id: "g600",
    name: "Gaseosa 600",
    price: { amount: 3200, currency: "COP" },
    productTypeId: "t-gaseosas",
    productTypeName: "Gaseosas",
    productVariantId: "v-original",
    productVariantName: "Original",
    presentation: { quantity: 600, unit: "ML" },
  }),
  product({
    id: "g15",
    name: "Gaseosa 1.5",
    price: { amount: 7500, currency: "COP" },
    productTypeId: "t-gaseosas",
    productTypeName: "Gaseosas",
    productVariantId: "v-original",
    productVariantName: "Original",
    presentation: { quantity: 1.5, unit: "L" },
  }),
  product({
    id: "gz600",
    name: "Gaseosa sin azúcar 600",
    price: { amount: 3500, currency: "COP" },
    productTypeId: "t-gaseosas",
    productTypeName: "Gaseosas",
    productVariantId: "v-sin-azucar",
    productVariantName: "Sin azúcar",
    presentation: { quantity: 600, unit: "ML" },
  }),
  product({
    id: "a600",
    name: "Agua 600",
    price: { amount: 2100, currency: "COP" },
    productTypeId: "t-agua",
    productTypeName: "Agua",
    productVariantId: "v-natural",
    productVariantName: "Natural",
    presentation: { quantity: 600, unit: "ML" },
  }),
  product({
    id: "huevos",
    name: "Huevos AAA",
    price: { amount: 18900, currency: "COP" },
    productTypeId: "t-huevos",
    productTypeName: "Huevos",
    // Producto sin variante: caso válido del contrato.
    productVariantId: null,
    productVariantName: null,
    presentation: { quantity: 30, unit: "UNIT" },
  }),
];

describe("product sorting", () => {
  it("offers only relevance and price sorting (no name)", () => {
    expect(PRODUCT_SORT_OPTIONS.map((option) => option.value)).toEqual([
      "relevance",
      "price-asc",
      "price-desc",
    ]);
  });

  it("sorts by price ascending and descending", () => {
    expect(sortProducts(catalog, "price-asc").map((item) => item.id)).toEqual([
      "a600", "g600", "gz600", "g15", "huevos",
    ]);
    expect(sortProducts(catalog, "price-desc").map((item) => item.id)).toEqual([
      "huevos", "g15", "gz600", "g600", "a600",
    ]);
  });
});

describe("type options come from real products", () => {
  it("collects each type once with its commercial name", () => {
    expect(collectProductTypeOptions(catalog)).toEqual([
      { value: "t-gaseosas", label: "Gaseosas" },
      { value: "t-agua", label: "Agua" },
      { value: "t-huevos", label: "Huevos" },
    ]);
  });

  it("skips products without type or without commercial name", () => {
    const options = collectProductTypeOptions([
      product({ id: "x", productTypeId: "t-x", productTypeName: null }),
      product({ id: "y" }),
    ]);
    expect(options).toEqual([]);
  });
});

describe("variant options depend on the selected type", () => {
  it("without type, lists every variant in the loaded products", () => {
    expect(collectProductVariantOptions(catalog, null)).toEqual([
      { value: "v-original", label: "Original" },
      { value: "v-sin-azucar", label: "Sin azúcar" },
      { value: "v-natural", label: "Natural" },
    ]);
  });

  it("with a type, only lists the variants of that type", () => {
    expect(collectProductVariantOptions(catalog, "t-gaseosas")).toEqual([
      { value: "v-original", label: "Original" },
      { value: "v-sin-azucar", label: "Sin azúcar" },
    ]);
    expect(collectProductVariantOptions(catalog, "t-agua")).toEqual([
      { value: "v-natural", label: "Natural" },
    ]);
  });

  it("never invents options for types without variants", () => {
    expect(collectProductVariantOptions(catalog, "t-huevos")).toEqual([]);
  });
});

describe("presentation options depend on type + variant", () => {
  it("lists presentations of the whole type when no variant is selected", () => {
    expect(collectPresentationOptions(catalog, "t-gaseosas", null)).toEqual([
      { value: "1.5:L", label: "1,5 L" },
      { value: "600:ML", label: "600 ml" },
    ]);
  });

  it("narrows presentations to type + variant combination", () => {
    expect(
      collectPresentationOptions(catalog, "t-gaseosas", "v-original"),
    ).toEqual([
      { value: "1.5:L", label: "1,5 L" },
      { value: "600:ML", label: "600 ml" },
    ]);
    expect(
      collectPresentationOptions(catalog, "t-gaseosas", "v-sin-azucar"),
    ).toEqual([{ value: "600:ML", label: "600 ml" }]);
  });

  it("does not mix presentations across types", () => {
    const aguaOptions = collectPresentationOptions(catalog, "t-agua", "v-natural");
    expect(aguaOptions).toEqual([{ value: "600:ML", label: "600 ml" }]);
  });
});

describe("presentation labels use shop language", () => {
  it("formats mass, volume and discrete units", () => {
    expect(formatPresentationLabel({ quantity: 600, unit: "ML" })).toBe("600 ml");
    expect(formatPresentationLabel({ quantity: 1.5, unit: "L" })).toBe("1,5 L");
    expect(formatPresentationLabel({ quantity: 2, unit: "KG" })).toBe("2 kg");
    expect(formatPresentationLabel({ quantity: 500, unit: "G" })).toBe("500 g");
    expect(formatPresentationLabel({ quantity: 1, unit: "UNIT" })).toBe("1 unidad");
    expect(formatPresentationLabel({ quantity: 6, unit: "UNIT" })).toBe("6 unidades");
    expect(formatPresentationLabel({ quantity: 30, unit: "UNIT" })).toBe("30 unidades");
  });

  it("builds stable keys from quantity and unit", () => {
    expect(presentationKeyOf({ quantity: 600, unit: "ML" })).toBe("600:ML");
  });
});

describe("cascade rules clear invalid descendants", () => {
  it("changing the type clears variant and presentation", () => {
    const current = {
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-gaseosas",
      productVariantId: "v-original",
      presentationKey: "600:ML",
      priceMin: 2000,
    };
    expect(withProductType(current, "t-agua")).toEqual({
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-agua",
      priceMin: 2000,
    });
  });

  it("changing the variant clears only the presentation", () => {
    const current = {
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-gaseosas",
      productVariantId: "v-original",
      presentationKey: "600:ML",
      priceMax: 9000,
    };
    expect(withProductVariant(current, "v-sin-azucar")).toEqual({
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-gaseosas",
      productVariantId: "v-sin-azucar",
      priceMax: 9000,
    });
  });

  it("keeps state untouched when the same value is reselected", () => {
    const current = {
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-gaseosas",
      productVariantId: "v-original",
      presentationKey: "600:ML",
    };
    expect(withProductType(current, "t-gaseosas")).toBe(current);
    expect(withProductVariant(current, "v-original")).toBe(current);
  });
});

describe("price input", () => {
  it("parses COP text with separators", () => {
    expect(parsePriceInput("5.000")).toBe(5000);
    expect(parsePriceInput("$ 20.500")).toBe(20500);
    expect(parsePriceInput("0")).toBe(0);
  });

  it("returns null for empty or non-numeric input", () => {
    expect(parsePriceInput("")).toBeNull();
    expect(parsePriceInput("abc")).toBeNull();
  });

  it("normalizes inverted bounds by swapping them", () => {
    expect(normalizePriceBounds(20000, 5000)).toEqual({ min: 5000, max: 20000 });
    expect(normalizePriceBounds(5000, null)).toEqual({ min: 5000, max: null });
  });

  it("formats the active range for the trigger and chips", () => {
    const both = formatPriceFilterLabel(5000, 20000);
    expect(both).toContain("5.000");
    expect(both).toContain("20.000");
    expect(both).toContain("–");
    expect(formatPriceFilterLabel(5000, null)).toContain("Desde");
    expect(formatPriceFilterLabel(null, 20000)).toContain("Hasta");
    expect(formatPriceFilterLabel(null, null)).toBe("Precio");
  });
});

describe("price slider bounds from real products", () => {
  it("derives min and max from the loaded catalog", () => {
    // Precios: 2100 … 18900 → min piso a $100, max techo a $1.000.
    expect(priceRangeBounds(catalog)).toEqual({ min: 2100, max: 19000 });
  });

  it("reserves a usable range when there is no price spread", () => {
    const flat = [1, 2, 3, 4].map((index) =>
      product({ id: `p${index}`, price: { amount: 5000, currency: "COP" } }),
    );
    expect(priceRangeBounds(flat)).toEqual({ min: 5000, max: 6000 });
  });

  it("returns null bounds for an empty catalog", () => {
    expect(priceRangeBounds([])).toBeNull();
  });
});

describe("filter application is an AND over real products", () => {
  it("filters by type", () => {
    const result = applyProductCatalogFilters(catalog, {
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-gaseosas",
    });
    expect(result.map((item) => item.id).sort()).toEqual(["g15", "g600", "gz600"]);
  });

  it("filters by type + variant", () => {
    const result = applyProductCatalogFilters(catalog, {
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-gaseosas",
      productVariantId: "v-original",
    });
    expect(result.map((item) => item.id).sort()).toEqual(["g15", "g600"]);
  });

  it("filters by type + variant + presentation", () => {
    const result = applyProductCatalogFilters(catalog, {
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-gaseosas",
      productVariantId: "v-original",
      presentationKey: presentationKeyOf({ quantity: 600, unit: "ML" }),
    });
    expect(result.map((item) => item.id)).toEqual(["g600"]);
  });

  it("an artificial combination yields zero products, never mixed data", () => {
    // Natural no pertenece a Gaseosas: la combinación no existe en los datos.
    const result = applyProductCatalogFilters(catalog, {
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-gaseosas",
      productVariantId: "v-natural",
    });
    expect(result).toEqual([]);
  });

  it("a corrupted presentation key yields zero products instead of crashing", () => {
    expect(
      applyProductCatalogFilters(catalog, {
        ...EMPTY_PRODUCT_CATALOG_FILTERS,
        presentationKey: "no-es-una-clave",
      }),
    ).toEqual([]);
  });

  it("composes presentation with price bounds", () => {
    const result = applyProductCatalogFilters(catalog, {
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-gaseosas",
      productVariantId: "v-original",
      priceMin: 5000,
      priceMax: null,
    });
    expect(result.map((item) => item.id)).toEqual(["g15"]);
  });

  it("products without variant survive when no variant filter is active", () => {
    const result = applyProductCatalogFilters(catalog, EMPTY_PRODUCT_CATALOG_FILTERS);
    expect(result.some((item) => item.id === "huevos")).toBe(true);
    const byType = applyProductCatalogFilters(catalog, {
      ...EMPTY_PRODUCT_CATALOG_FILTERS,
      productTypeId: "t-huevos",
    });
    expect(byType.map((item) => item.id)).toEqual(["huevos"]);
  });

  it("tracks whether any filter is active", () => {
    expect(hasActiveProductFilters(EMPTY_PRODUCT_CATALOG_FILTERS)).toBe(false);
    expect(
      hasActiveProductFilters({
        ...EMPTY_PRODUCT_CATALOG_FILTERS,
        presentationKey: "600:ML",
      }),
    ).toBe(true);
    expect(
      hasActiveProductFilters({ ...EMPTY_PRODUCT_CATALOG_FILTERS, priceMin: 1000 }),
    ).toBe(true);
  });
});
