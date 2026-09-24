import { describe, expect, it } from "vitest";
import {
  adminProductTypeFormValuesFromProductType,
  createAdminProductTypeRequestFromValues,
  emptyAdminProductTypeFormValues,
  updateAdminProductTypeRequestFromValues,
  validateAdminProductType,
} from "@/features/admin/payloads";
import type { ProductType } from "@/features/admin/api";

const CATEGORY_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

const productType: ProductType = {
  id: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  categoryId: CATEGORY_ID,
  name: "Leche entera",
  description: "Presentaciones de leche",
  status: "ACTIVE",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-02T00:00:00Z",
};

describe("admin product type payloads", () => {
  it("create payload binds categoryId from context and omits status", () => {
    const body = createAdminProductTypeRequestFromValues(CATEGORY_ID, {
      name: "Leche entera",
      description: "Presentaciones de leche",
    });

    expect(body).toEqual({
      categoryId: CATEGORY_ID,
      name: "Leche entera",
      description: "Presentaciones de leche",
    });
    expect(JSON.stringify(body)).not.toContain("status");
  });

  it("update payload excludes categoryId and status", () => {
    const body = updateAdminProductTypeRequestFromValues({
      name: "Leche semidescremada",
      description: "",
    });

    expect(body).toEqual({
      name: "Leche semidescremada",
      description: null,
    });
    expect(JSON.stringify(body)).not.toContain("categoryId");
    expect(JSON.stringify(body)).not.toContain("status");
  });

  it("validates name is required", () => {
    expect(
      validateAdminProductType(emptyAdminProductTypeFormValues()),
    ).toMatchObject({
      name: expect.any(String),
    });
    expect(
      validateAdminProductType({
        name: "Ok",
        description: "",
      }),
    ).toEqual({});
  });

  it("maps product type into form values without status", () => {
    const values = adminProductTypeFormValuesFromProductType(productType);
    expect(values).toEqual({
      name: "Leche entera",
      description: "Presentaciones de leche",
    });
    expect(values).not.toHaveProperty("categoryId");
    expect(values).not.toHaveProperty("status");
  });
});
