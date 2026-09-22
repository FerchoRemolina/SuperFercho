import { describe, expect, it } from "vitest";
import {
  createAdminCategoryRequestFromValues,
  emptyAdminCategoryFormValues,
  updateAdminCategoryRequestFromValues,
  validateAdminCategory,
} from "@/features/admin/payloads";

describe("admin category payloads", () => {
  it("create payload includes name and optional description without status", () => {
    const body = createAdminCategoryRequestFromValues({
      name: "Lácteos",
      description: "Leche y derivados",
    });

    expect(body).toEqual({
      name: "Lácteos",
      description: "Leche y derivados",
    });
    expect(JSON.stringify(body)).not.toContain("status");
  });

  it("update payload excludes status", () => {
    const body = updateAdminCategoryRequestFromValues({
      name: "Lácteos frescos",
      description: "",
    });

    expect(body).toEqual({
      name: "Lácteos frescos",
      description: null,
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
      }),
    ).toEqual({});
  });
});
