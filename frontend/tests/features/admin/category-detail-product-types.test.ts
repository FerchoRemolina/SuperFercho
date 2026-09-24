import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { adminKeys } from "@/features/admin/api";
import { ADMIN_NAV_LINKS } from "@/features/admin/presentation";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("admin category detail product types composition", () => {
  it("renders Tipos de producto section on category detail only", () => {
    const detail = source(
      "features/admin/components/admin-category-detail-page.tsx",
    );
    const section = source(
      "features/admin/components/admin-product-type-section.tsx",
    );
    const hub = source("features/admin/components/admin-hub.tsx");

    expect(detail).toContain("AdminProductTypeSection");
    expect(detail).toContain("categoryId={category.id}");
    expect(section).toContain("Tipos de producto");
    expect(section).toContain("useAdminProductTypesQuery(categoryId)");
    expect(hub).not.toContain("product-types");
    expect(hub).not.toContain("Tipos de producto");
    expect(
      ADMIN_NAV_LINKS.some((link) => link.href.includes("product-type")),
    ).toBe(false);
  });

  it("covers loading, error retry and empty states", () => {
    const section = source(
      "features/admin/components/admin-product-type-section.tsx",
    );
    expect(section).toContain("typesQuery.isPending");
    expect(section).toContain("Skeleton");
    expect(section).toContain("typesQuery.isError");
    expect(section).toContain("Reintentar");
    expect(section).toContain("typesQuery.refetch()");
    expect(section).toContain("No hay tipos de producto");
    expect(section).toContain("EmptyState");
  });

  it("wires create, edit, activate and deactivate without category picker", () => {
    const section = source(
      "features/admin/components/admin-product-type-section.tsx",
    );
    const form = source(
      "features/admin/components/admin-product-type-form.tsx",
    );
    const statusActions = source(
      "features/admin/components/admin-product-type-status-actions.tsx",
    );
    const payloads = source("features/admin/payloads.ts");

    expect(section).toContain("useCreateAdminProductTypeMutation");
    expect(section).toContain("useUpdateAdminProductTypeMutation");
    expect(section).toContain("createAdminProductTypeRequestFromValues");
    expect(section).toContain("categoryId,");
    expect(section).toContain("AdminProductTypeStatusActions");
    expect(statusActions).toContain("useActivateAdminProductTypeMutation");
    expect(statusActions).toContain("useDeactivateAdminProductTypeMutation");
    expect(form).not.toContain("categoryId");
    expect(payloads).toContain(
      "categoryId comes from Category Detail context",
    );
  });

  it("keeps product-type list query keyed by categoryId", () => {
    const hooks = source("features/admin/hooks.ts");
    expect(hooks).toContain("keys.productTypes(categoryId)");
    expect(hooks).toContain(
      "enabled: isAdminRole(session?.role) && categoryId.length > 0",
    );
    expect(adminKeys().productTypes("cat-1")).toEqual([
      "admin",
      "product-types",
      "list",
      "cat-1",
    ]);
  });

  it("invalidates product-type queries after mutations", () => {
    const hooks = source("features/admin/hooks.ts");
    expect(hooks).toContain("keys.productTypesRoot()");
    expect(hooks).toContain("keys.productTypes(productType.categoryId)");
    expect(hooks).toContain("keys.productType(productType.id)");
  });

  it("nests ProductVariant management under each ProductType without top-level routes", () => {
    const section = source(
      "features/admin/components/admin-product-type-section.tsx",
    );
    const detail = source(
      "features/admin/components/admin-category-detail-page.tsx",
    );
    expect(section).toContain("AdminProductVariantSection");
    expect(section).toContain("productTypeId={productType.id}");
    expect(section).not.toContain("Próximamente");
    expect(section).not.toContain("Coming soon");
    expect(detail).not.toContain("/admin/product-types");
    expect(section).not.toContain("/admin/product-types");
    expect(section).not.toContain("/admin/product-variants");
  });
});
