import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  createAdminProductRequestFromValues,
  emptyAdminProductFormValues,
  updateAdminProductRequestFromValues,
} from "@/features/admin/payloads";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

const TYPE_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

describe("admin product form taxonomy composition", () => {
  it("orders Category → Type → Variant → Presentation before commercial fields", () => {
    const form = source("features/admin/components/admin-product-form.tsx");
    const categoryIdx = form.indexOf('label="Categoría"');
    const typeIdx = form.indexOf('label="Tipo de producto"');
    const variantIdx = form.indexOf('label="Variante (opcional)"');
    const quantityIdx = form.indexOf('label="Cantidad de presentación"');
    const unitIdx = form.indexOf('label="Unidad"');
    const nameIdx = form.indexOf('label="Nombre"');

    expect(categoryIdx).toBeGreaterThan(-1);
    expect(typeIdx).toBeGreaterThan(categoryIdx);
    expect(variantIdx).toBeGreaterThan(typeIdx);
    expect(quantityIdx).toBeGreaterThan(variantIdx);
    expect(unitIdx).toBeGreaterThan(quantityIdx);
    expect(nameIdx).toBeGreaterThan(unitIdx);
  });

  it("loads types and variants with gated queries and cascade clears", () => {
    const form = source("features/admin/components/admin-product-form.tsx");
    expect(form).toContain("useAdminProductTypesQuery(values.categoryId)");
    expect(form).toContain("useAdminProductVariantsQuery(values.productTypeId)");
    expect(form).toContain("withAdminProductCategoryId");
    expect(form).toContain("withAdminProductTypeId");
    expect(form).toContain("Sin variante");
    expect(form).toContain("— Inactiva");
    expect(form).toContain("PRESENTATION_UNITS");
  });

  it("keeps Open Food Facts from driving taxonomy on create page", () => {
    const createPage = source(
      "features/admin/components/admin-create-product-page.tsx",
    );
    const payloads = source("features/admin/payloads.ts");
    expect(createPage).toContain("applyBarcodeSuggestion");
    expect(createPage).toContain("lookupProductByBarcode");
    expect(payloads).toContain(
      "Never touches price, stock, categoryId, productTypeId, presentation",
    );
  });

  it("create and update payloads omit categoryId and serialize presentation", () => {
    const createBody = createAdminProductRequestFromValues({
      ...emptyAdminProductFormValues(),
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      productTypeId: TYPE_ID,
      productVariantId: "",
      presentationQuantity: "1",
      presentationUnit: "L",
      name: "Leche",
      price: "15900",
      stock: "20",
    });
    expect(createBody).not.toHaveProperty("categoryId");
    expect(createBody.productTypeId).toBe(TYPE_ID);
    expect(createBody.productVariantId).toBeNull();
    expect(createBody.presentation).toEqual({ quantity: 1, unit: "L" });

    const updateBody = updateAdminProductRequestFromValues({
      ...emptyAdminProductFormValues(),
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      productTypeId: TYPE_ID,
      productVariantId: "",
      presentationQuantity: "900",
      presentationUnit: "ML",
      name: "Leche",
    });
    expect(updateBody).not.toHaveProperty("categoryId");
    expect(updateBody.productVariantId).toBeNull();
    expect(updateBody.presentation).toEqual({ quantity: 900, unit: "ML" });
  });
});
