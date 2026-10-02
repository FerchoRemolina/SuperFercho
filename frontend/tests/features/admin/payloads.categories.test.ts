import { describe, expect, it } from "vitest";
import {
  createAdminCategoryRequestFromValues,
  emptyAdminCategoryFormValues,
  updateAdminCategoryRequestFromValues,
  validateAdminCategory,
} from "@/features/admin/payloads";

describe("admin category payloads", () => {
  it("create payload includes name, optional description and icon without status", () => {
    const body = createAdminCategoryRequestFromValues({
      name: "Lácteos",
      description: "Leche y derivados",
      icon: "DAIRY",
    });

    expect(body).toEqual({
      name: "Lácteos",
      description: "Leche y derivados",
      icon: "DAIRY",
    });
    expect(JSON.stringify(body)).not.toContain("status");
  });

  it("update payload excludes status", () => {
    const body = updateAdminCategoryRequestFromValues({
      name: "Lácteos frescos",
      description: "",
      icon: "OTHER",
    });

    expect(body).toEqual({
      name: "Lácteos frescos",
      description: null,
      icon: "OTHER",
    });
    expect(JSON.stringify(body)).not.toContain("status");
  });

  it("validates name is required", () => {
    expect(validateAdminCategory(emptyAdminCategoryFormValues())).toMatchObject({
      name: expect.any(String),
    });
    expect(
      validateAdminCategory({
        name: "Ok",
        description: "",
        icon: "OTHER",
      }),
    ).toEqual({});
  });

  it("validates icon belongs to the closed set", () => {
    const errors = validateAdminCategory({
      ...emptyAdminCategoryFormValues(),
      icon: "NOT_AN_ICON" as never,
    });
    expect(errors.icon).toEqual(expect.any(String));
  });
});
