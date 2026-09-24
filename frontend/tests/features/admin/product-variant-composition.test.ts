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

describe("admin product variant composition", () => {
  it("nests variants under each product type with optional empty state", () => {
    const typeSection = source(
      "features/admin/components/admin-product-type-section.tsx",
    );
    const variantSection = source(
      "features/admin/components/admin-product-variant-section.tsx",
    );

    expect(typeSection).toContain("AdminProductVariantSection");
    expect(typeSection).toContain("productTypeId={productType.id}");
    expect(variantSection).toContain("Variantes");
    expect(variantSection).toContain("Nueva variante");
    expect(variantSection).toContain("No hay variantes");
    expect(variantSection).toContain("son opcionales");
    expect(variantSection).toContain(
      "useAdminProductVariantsQuery(productTypeId)",
    );
  });

  it("covers loading, error retry and empty states", () => {
    const section = source(
      "features/admin/components/admin-product-variant-section.tsx",
    );
    expect(section).toContain("variantsQuery.isPending");
    expect(section).toContain("Skeleton");
    expect(section).toContain("variantsQuery.isError");
    expect(section).toContain("Reintentar");
    expect(section).toContain("variantsQuery.refetch()");
    expect(section).toContain("EmptyState");
  });

  it("wires create, edit, activate and deactivate without productType picker", () => {
    const section = source(
      "features/admin/components/admin-product-variant-section.tsx",
    );
    const form = source(
      "features/admin/components/admin-product-variant-form.tsx",
    );
    const statusActions = source(
      "features/admin/components/admin-product-variant-status-actions.tsx",
    );
    const payloads = source("features/admin/payloads.ts");

    expect(section).toContain("useCreateAdminProductVariantMutation");
    expect(section).toContain("useUpdateAdminProductVariantMutation");
    expect(section).toContain("createAdminProductVariantRequestFromValues");
    expect(section).toContain("productTypeId,");
    expect(section).toContain("AdminProductVariantStatusActions");
    expect(statusActions).toContain("useActivateAdminProductVariantMutation");
    expect(statusActions).toContain("useDeactivateAdminProductVariantMutation");
    expect(form).not.toContain("productTypeId");
    expect(payloads).toContain(
      "productTypeId comes from the parent ProductType",
    );
  });

  it("keeps variant list query keyed and enabled by productTypeId", () => {
    const hooks = source("features/admin/hooks.ts");
    expect(hooks).toContain("keys.productVariants(productTypeId)");
    expect(hooks).toContain(
      "enabled: isAdminRole(session?.role) && productTypeId.length > 0",
    );
    expect(adminKeys().productVariants("type-1")).toEqual([
      "admin",
      "product-variants",
      "list",
      "type-1",
    ]);
  });

  it("invalidates variant queries after mutations", () => {
    const hooks = source("features/admin/hooks.ts");
    expect(hooks).toContain("keys.productVariantsRoot()");
    expect(hooks).toContain(
      "keys.productVariants(productVariant.productTypeId)",
    );
    expect(hooks).toContain("keys.productVariant(productVariant.id)");
  });

  it("shows status badge for active and inactive variants in admin", () => {
    const section = source(
      "features/admin/components/admin-product-variant-section.tsx",
    );
    const badge = source(
      "features/admin/components/product-variant-status-badge.tsx",
    );
    expect(section).toContain("ProductVariantStatusBadge");
    expect(section).toContain("productVariant.status");
    expect(badge).toContain('status === "ACTIVE"');
    expect(badge).toContain("productVariantStatusLabel");
    expect(section).not.toContain(".filter(");
    expect(section).not.toContain('status !== "INACTIVE"');
  });

  it("does not add top-level navigation or routes for variants", () => {
    const hub = source("features/admin/components/admin-hub.tsx");
    const section = source(
      "features/admin/components/admin-product-variant-section.tsx",
    );
    expect(hub).not.toContain("product-variants");
    expect(hub).not.toContain("Variantes");
    expect(section).not.toContain("/admin/product-variants");
    expect(section).not.toContain("Próximamente");
    expect(section).not.toContain("Coming soon");
    expect(
      ADMIN_NAV_LINKS.some((link) => link.href.includes("product-variant")),
    ).toBe(false);
  });
});
