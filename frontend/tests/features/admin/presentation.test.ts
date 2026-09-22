import { describe, expect, it } from "vitest";
import {
  adminProductsHref,
  isAdminRole,
  listQueryFromSearchParams,
  shouldSearchAdminProducts,
} from "@/features/admin/presentation";

describe("admin presentation", () => {
  it("detects admin role", () => {
    expect(isAdminRole("ADMIN")).toBe(true);
    expect(isAdminRole("CUSTOMER")).toBe(false);
    expect(isAdminRole(undefined)).toBe(false);
  });

  it("builds list href with filters when not searching", () => {
    expect(
      adminProductsHref({
        categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        status: "INACTIVE",
      }),
    ).toBe(
      "/admin/products?categoryId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa&status=INACTIVE",
    );
  });

  it("builds search href without category or status filters", () => {
    expect(
      adminProductsHref({
        text: "leche",
        categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        status: "ACTIVE",
      }),
    ).toBe("/admin/products?text=leche");
  });

  it("parses list query from search params", () => {
    expect(
      listQueryFromSearchParams({
        categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        status: "ACTIVE",
      }),
    ).toEqual({
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      status: "ACTIVE",
    });
    expect(listQueryFromSearchParams({ status: "UNKNOWN" })).toEqual({});
  });

  it("requires non-blank text to search", () => {
    expect(shouldSearchAdminProducts("  leche ")).toBe(true);
    expect(shouldSearchAdminProducts("   ")).toBe(false);
  });
});
