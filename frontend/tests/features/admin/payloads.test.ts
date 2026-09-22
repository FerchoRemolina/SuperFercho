import { describe, expect, it } from "vitest";
import {
  changeAdminProductPriceRequestFromAmount,
  createAdminProductRequestFromValues,
  emptyAdminProductFormValues,
  updateAdminProductRequestFromValues,
  validateCreateAdminProduct,
  validateUpdateAdminProduct,
} from "@/features/admin/payloads";

describe("admin product payloads", () => {
  it("create payload includes stock and price", () => {
    const body = createAdminProductRequestFromValues({
      ...emptyAdminProductFormValues(),
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      name: "Leche",
      price: "10.50",
      stock: "12",
    });

    expect(body).toEqual({
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      barcode: null,
      name: "Leche",
      brand: null,
      description: null,
      price: { amount: 10.5, currency: "COP" },
      stock: 12,
      imageUrl: null,
    });
  });

  it("update payload excludes stock, price, and status", () => {
    const body = updateAdminProductRequestFromValues({
      ...emptyAdminProductFormValues(),
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      name: "Leche entera",
      brand: "Marca",
      barcode: "770123",
      description: "Descripción",
      price: "99.99",
      stock: "50",
      imageUrl: "https://img.test/x.png",
    });

    expect(body).toEqual({
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      barcode: "770123",
      name: "Leche entera",
      brand: "Marca",
      description: "Descripción",
      imageUrl: "https://img.test/x.png",
    });
    expect(JSON.stringify(body)).not.toContain("stock");
    expect(JSON.stringify(body)).not.toContain("price");
    expect(JSON.stringify(body)).not.toContain("status");
  });

  it("change price payload uses only the price field", () => {
    expect(changeAdminProductPriceRequestFromAmount("15")).toEqual({
      price: { amount: 15, currency: "COP" },
    });
  });

  it("validates create requires name, category, price, and stock", () => {
    expect(validateCreateAdminProduct(emptyAdminProductFormValues())).toMatchObject({
      name: expect.any(String),
      categoryId: expect.any(String),
      price: expect.any(String),
      stock: expect.any(String),
    });
  });

  it("validates update requires name and category only", () => {
    const errors = validateUpdateAdminProduct({
      ...emptyAdminProductFormValues(),
      name: "Ok",
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      price: "",
      stock: "",
    });
    expect(errors).toEqual({});
  });
});
