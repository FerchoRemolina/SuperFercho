import { describe, expect, it } from "vitest";
import {
  adminOrderDetailHref,
  adminOrdersHref,
  adminOrdersListQueryFromSearchParams,
  adminOrdersPageCount,
  adminProductsHref,
  canGoToNextAdminOrdersPage,
  canGoToPreviousAdminOrdersPage,
  isAdminRole,
  listQueryFromSearchParams,
  parseAdminOrderStatus,
  parseAdminOrdersPage,
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

describe("admin orders presentation", () => {
  it("parses known order statuses and rejects unknown ones", () => {
    expect(parseAdminOrderStatus("CONFIRMED")).toBe("CONFIRMED");
    expect(parseAdminOrderStatus("SOLD")).toBeUndefined();
    expect(parseAdminOrderStatus("")).toBeUndefined();
    expect(parseAdminOrderStatus(null)).toBeUndefined();
  });

  it("parses page from search params with default 0", () => {
    expect(parseAdminOrdersPage(null)).toBe(0);
    expect(parseAdminOrdersPage("2")).toBe(2);
    expect(parseAdminOrdersPage("-1")).toBe(0);
    expect(parseAdminOrdersPage("abc")).toBe(0);
  });

  it("builds list query with default size 20", () => {
    expect(
      adminOrdersListQueryFromSearchParams({
        page: "1",
        status: "PREPARING",
      }),
    ).toEqual({ page: 1, size: 20, status: "PREPARING" });
    expect(adminOrdersListQueryFromSearchParams({})).toEqual({
      page: 0,
      size: 20,
      status: undefined,
    });
  });

  it("builds orders href with page and status filters", () => {
    expect(adminOrdersHref({})).toBe("/admin/orders");
    expect(adminOrdersHref({ page: 0, status: "" })).toBe("/admin/orders");
    expect(adminOrdersHref({ page: 2, status: "READY" })).toBe(
      "/admin/orders?page=2&status=READY",
    );
    expect(adminOrdersHref({ page: 0, status: "CANCELLED" })).toBe(
      "/admin/orders?status=CANCELLED",
    );
  });

  it("computes pagination bounds from totalElements", () => {
    expect(adminOrdersPageCount(0, 20)).toBe(0);
    expect(adminOrdersPageCount(20, 20)).toBe(1);
    expect(adminOrdersPageCount(21, 20)).toBe(2);
    expect(canGoToPreviousAdminOrdersPage(0)).toBe(false);
    expect(canGoToPreviousAdminOrdersPage(1)).toBe(true);
    expect(canGoToNextAdminOrdersPage(0, 20, 20)).toBe(false);
    expect(canGoToNextAdminOrdersPage(0, 20, 21)).toBe(true);
    expect(canGoToNextAdminOrdersPage(1, 20, 21)).toBe(false);
  });

  it("builds admin order detail href", () => {
    expect(
      adminOrderDetailHref("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ).toBe("/admin/orders/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  });
});
