import { describe, expect, it } from "vitest";
import type { ProductVariant } from "@/features/admin/api";
import {
  adminProductVariantFormValuesFromProductVariant,
  createAdminProductVariantRequestFromValues,
  emptyAdminProductVariantFormValues,
  updateAdminProductVariantRequestFromValues,
  validateAdminProductVariant,
} from "@/features/admin/payloads";

const TYPE_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

const productVariant: ProductVariant = {
  id: "cccccccc-cccc-cccc-cccc-cccccccccccc",
  productTypeId: TYPE_ID,
  name: "Entera",
  description: "Leche entera",
  status: "INACTIVE",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-02T00:00:00Z",
};

describe("admin product variant payloads", () => {
  it("create payload binds productTypeId from parent and omits status", () => {
    const body = createAdminProductVariantRequestFromValues(TYPE_ID, {
      name: "Entera",
      description: "Leche entera",
    });

    expect(body).toEqual({
      productTypeId: TYPE_ID,
      name: "Entera",
      description: "Leche entera",
    });
    expect(JSON.stringify(body)).not.toContain("status");
  });

  it("update payload excludes productTypeId and status", () => {
    const body = updateAdminProductVariantRequestFromValues({
      name: "Deslactosada",
      description: "",
    });

    expect(body).toEqual({
      name: "Deslactosada",
      description: null,
    });
    expect(JSON.stringify(body)).not.toContain("productTypeId");
    expect(JSON.stringify(body)).not.toContain("status");
  });

  it("validates name is required", () => {
    expect(
      validateAdminProductVariant(emptyAdminProductVariantFormValues()),
    ).toMatchObject({
      name: expect.any(String),
    });
    expect(
      validateAdminProductVariant({
        name: "Ok",
        description: "",
      }),
    ).toEqual({});
  });

  it("maps inactive variant into form values without dropping it from admin", () => {
    expect(productVariant.status).toBe("INACTIVE");
    const values = adminProductVariantFormValuesFromProductVariant(productVariant);
    expect(values).toEqual({
      name: "Entera",
      description: "Leche entera",
    });
    expect(values).not.toHaveProperty("productTypeId");
    expect(values).not.toHaveProperty("status");
  });
});
