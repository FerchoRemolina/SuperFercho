import { describe, expect, it } from "vitest";
import {
  applyBarcodeSuggestion,
  adjustAdminProductStockRequestFromValue,
  adminProductFormValuesFromProduct,
  changeAdminProductPriceRequestFromAmount,
  createAdminKnowledgeDocumentRequestFromValues,
  createAdminProductRequestFromValues,
  emptyAdminKnowledgeDocumentContentFormValues,
  emptyAdminKnowledgeDocumentFormValues,
  emptyAdminProductFormValues,
  parsePresentationQuantity,
  replaceAdminKnowledgeDocumentContentRequestFromValues,
  updateAdminProductRequestFromValues,
  validateAdminKnowledgeDocument,
  validateAdminKnowledgeDocumentContent,
  validateAdminProductStock,
  validateCreateAdminProduct,
  validateUpdateAdminProduct,
  withAdminProductCategoryId,
  withAdminProductTypeId,
} from "@/features/admin/payloads";

const TYPE_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
const VARIANT_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";

describe("admin product payloads", () => {
  it("create payload includes type, nullable variant, presentation, stock and price", () => {
    const body = createAdminProductRequestFromValues({
      ...emptyAdminProductFormValues(),
      productTypeId: TYPE_ID,
      productVariantId: "",
      presentationQuantity: "1.5",
      presentationUnit: "L",
      name: "Leche",
      price: "10.50",
      stock: "12",
    });

    expect(body).toEqual({
      productTypeId: TYPE_ID,
      productVariantId: null,
      presentation: { quantity: 1.5, unit: "L" },
      barcode: null,
      name: "Leche",
      brand: null,
      description: null,
      price: { amount: 10.5, currency: "COP" },
      stock: 12,
      imageUrl: null,
    });
    expect(body).not.toHaveProperty("categoryId");
  });

  it("create payload keeps productVariantId when present", () => {
    const body = createAdminProductRequestFromValues({
      ...emptyAdminProductFormValues(),
      productTypeId: TYPE_ID,
      productVariantId: VARIANT_ID,
      presentationQuantity: "1",
      presentationUnit: "UNIT",
      name: "Leche",
      price: "10",
      stock: "1",
    });

    expect(body.productVariantId).toBe(VARIANT_ID);
    expect(body.presentation).toEqual({ quantity: 1, unit: "UNIT" });
  });

  it("update payload excludes stock, price, status, and categoryId", () => {
    const body = updateAdminProductRequestFromValues({
      ...emptyAdminProductFormValues(),
      productTypeId: TYPE_ID,
      productVariantId: VARIANT_ID,
      presentationQuantity: "900",
      presentationUnit: "ML",
      name: "Leche entera",
      brand: "Marca",
      barcode: "770123",
      description: "Descripción",
      price: "99.99",
      stock: "50",
      imageUrl: "https://img.test/x.png",
    });

    expect(body).toEqual({
      productTypeId: TYPE_ID,
      productVariantId: VARIANT_ID,
      presentation: { quantity: 900, unit: "ML" },
      barcode: "770123",
      name: "Leche entera",
      brand: "Marca",
      description: "Descripción",
      imageUrl: "https://img.test/x.png",
    });
    expect(JSON.stringify(body)).not.toContain("stock");
    expect(JSON.stringify(body)).not.toContain("price");
    expect(JSON.stringify(body)).not.toContain("status");
    expect(JSON.stringify(body)).not.toContain("categoryId");
  });

  it("change price payload uses only the price field", () => {
    expect(changeAdminProductPriceRequestFromAmount("15")).toEqual({
      price: { amount: 15, currency: "COP" },
    });
  });

  it("builds stock adjust payload and validates stock", () => {
    expect(adjustAdminProductStockRequestFromValue("25")).toEqual({ stock: 25 });
    expect(validateAdminProductStock("0")).toBeUndefined();
    expect(validateAdminProductStock("-1")).toMatch(/stock/i);
    expect(validateAdminProductStock("1.5")).toMatch(/stock/i);
  });

  it("validates create requires category, name, productType, presentation, price, and stock", () => {
    expect(validateCreateAdminProduct(emptyAdminProductFormValues())).toMatchObject({
      name: expect.any(String),
      categoryId: expect.any(String),
      productTypeId: expect.any(String),
      price: expect.any(String),
      stock: expect.any(String),
    });
    expect(
      validateCreateAdminProduct({
        ...emptyAdminProductFormValues(),
        presentationQuantity: "0",
        presentationUnit: "",
      }),
    ).toMatchObject({
      presentationQuantity: expect.any(String),
      presentationUnit: expect.any(String),
    });
  });

  it("validates update requires category, name, productType and presentation only", () => {
    const errors = validateUpdateAdminProduct({
      ...emptyAdminProductFormValues(),
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      name: "Ok",
      productTypeId: TYPE_ID,
      presentationQuantity: "1",
      presentationUnit: "UNIT",
      price: "",
      stock: "",
    });
    expect(errors).toEqual({});
  });

  it("rejects product content fields that exceed catalog limits", () => {
    const base = {
      ...emptyAdminProductFormValues(),
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      productTypeId: TYPE_ID,
      presentationQuantity: "1",
      presentationUnit: "UNIT" as const,
      price: "1000",
      stock: "1",
    };
    expect(
      validateCreateAdminProduct({
        ...base,
        name: "n".repeat(31),
      }),
    ).toMatchObject({ name: expect.stringMatching(/30/i) });
    expect(
      validateCreateAdminProduct({
        ...base,
        name: "Ok",
        brand: "b".repeat(31),
      }),
    ).toMatchObject({ brand: expect.stringMatching(/30/i) });
    expect(
      validateUpdateAdminProduct({
        ...base,
        name: "Ok",
        description: "d".repeat(201),
      }),
    ).toMatchObject({ description: expect.stringMatching(/200/i) });
    expect(
      validateCreateAdminProduct({
        ...base,
        name: "n".repeat(30),
        brand: "b".repeat(30),
        description: "d".repeat(200),
      }),
    ).toEqual({});
  });

  it("keeps overlong barcode suggestions and blocks save via validation", () => {
    const longName = "n".repeat(40);
    const longBrand = "b".repeat(40);
    const longDescription = "d".repeat(250);
    const values = applyBarcodeSuggestion(emptyAdminProductFormValues(), {
      barcode: "7701234567890",
      name: longName,
      brand: longBrand,
      description: longDescription,
      imageUrl: null,
    });

    expect(values.name).toBe(longName);
    expect(values.brand).toBe(longBrand);
    expect(values.description).toBe(longDescription);
    expect(
      validateCreateAdminProduct({
        ...values,
        categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        productTypeId: TYPE_ID,
        presentationQuantity: "1",
        presentationUnit: "UNIT",
        price: "1000",
        stock: "1",
      }),
    ).toMatchObject({
      name: expect.any(String),
      brand: expect.any(String),
      description: expect.any(String),
    });
  });

  it("parses presentation quantity with up to three decimals and rejects zero", () => {
    expect(parsePresentationQuantity("1.125")).toBe(1.125);
    expect(parsePresentationQuantity("1.1234")).toBeNull();
    expect(parsePresentationQuantity("0")).toBeNull();
  });

  it("clears type and variant when category changes", () => {
    const next = withAdminProductCategoryId(
      {
        ...emptyAdminProductFormValues(),
        categoryId: "cat-old",
        productTypeId: TYPE_ID,
        productVariantId: VARIANT_ID,
      },
      "cat-new",
    );
    expect(next.categoryId).toBe("cat-new");
    expect(next.productTypeId).toBe("");
    expect(next.productVariantId).toBe("");
  });

  it("clears variant when type changes", () => {
    const next = withAdminProductTypeId(
      {
        ...emptyAdminProductFormValues(),
        categoryId: "cat-1",
        productTypeId: TYPE_ID,
        productVariantId: VARIANT_ID,
      },
      "type-new",
    );
    expect(next.productTypeId).toBe("type-new");
    expect(next.productVariantId).toBe("");
  });

  it("maps product with and without variant into edit form values", () => {
    const baseProduct = {
      id: "p1",
      categoryId: "cat-1",
      productTypeId: TYPE_ID,
      presentation: { quantity: 1, unit: "L" as const },
      barcode: null,
      name: "Leche",
      brand: null,
      description: null,
      price: { amount: 10, currency: "COP" as const },
      stock: 1,
      imageUrl: null,
      status: "ACTIVE" as const,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    };

    const withVariant = adminProductFormValuesFromProduct({
      ...baseProduct,
      productVariantId: VARIANT_ID,
    });
    expect(withVariant.categoryId).toBe("cat-1");
    expect(withVariant.productTypeId).toBe(TYPE_ID);
    expect(withVariant.productVariantId).toBe(VARIANT_ID);
    expect(withVariant.presentationUnit).toBe("L");

    const withoutVariant = adminProductFormValuesFromProduct({
      ...baseProduct,
      id: "p2",
      productVariantId: null,
      presentation: { quantity: 500, unit: "G" },
      name: "Harina",
    });
    expect(withoutVariant.productVariantId).toBe("");
    expect(withoutVariant.presentationQuantity).toBe("500");
    expect(withoutVariant.presentationUnit).toBe("G");
  });
});

describe("admin knowledge payloads", () => {
  it("rejects blank title source and content", () => {
    expect(
      validateAdminKnowledgeDocument(emptyAdminKnowledgeDocumentFormValues()),
    ).toEqual({
      title: expect.any(String),
      source: expect.any(String),
      content: expect.any(String),
    });
  });

  it("maps valid create values to request body", () => {
    expect(
      createAdminKnowledgeDocumentRequestFromValues({
        title: "  Horarios ",
        source: " manual ",
        content: " Texto ",
      }),
    ).toEqual({
      title: "Horarios",
      source: "manual",
      content: "Texto",
    });
  });

  it("allows submit when title source and content are present", () => {
    expect(
      validateAdminKnowledgeDocument({
        title: "Horarios",
        source: "manual-interno",
        content: "Abrimos de 8 a 20.",
      }),
    ).toEqual({});
  });

  it("rejects blank replace content and maps valid content", () => {
    expect(
      validateAdminKnowledgeDocumentContent(
        emptyAdminKnowledgeDocumentContentFormValues(),
      ),
    ).toEqual({ content: expect.any(String) });
    expect(
      replaceAdminKnowledgeDocumentContentRequestFromValues({
        content: "  Nuevo contenido ",
      }),
    ).toEqual({ content: "Nuevo contenido" });
  });
});
