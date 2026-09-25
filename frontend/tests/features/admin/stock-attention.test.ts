import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  ADMIN_STOCK_LOW_EMPTY_MESSAGE,
  ADMIN_STOCK_OUT_EMPTY_MESSAGE,
} from "@/features/admin/presentation";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("admin stock attention panel", () => {
  it("composes the panel under Hub section cards without a stock route", () => {
    const hub = source("features/admin/components/admin-hub.tsx");
    const panel = source(
      "features/admin/components/admin-stock-attention-panel.tsx",
    );

    expect(hub).toContain("AdminStockAttentionPanel");
    expect(hub).not.toContain("/admin/stock");
    expect(panel).toContain('useAdminProductsQuery({ status: "ACTIVE" })');
    expect(panel).toContain("partitionAdminStockAttention");
    expect(panel).toContain("Próximos a agotarse");
    expect(panel).toContain("Agotados");
    expect(panel).toContain("ADMIN_STOCK_LOW_EMPTY_MESSAGE");
    expect(panel).toContain("ADMIN_STOCK_OUT_EMPTY_MESSAGE");
    expect(panel).toContain("No se pudieron cargar los productos.");
    expect(ADMIN_STOCK_LOW_EMPTY_MESSAGE).toBe(
      "No hay productos activos con stock bajo (1 a 5 unidades).",
    );
    expect(ADMIN_STOCK_OUT_EMPTY_MESSAGE).toBe(
      "No hay productos activos agotados.",
    );
    expect(panel).not.toContain("Ver todos");
    expect(panel).not.toContain("adjustAdminProductStock");
    expect(panel).not.toContain("ProductStockPanel");
  });

  it("links Ver producto to the Admin product detail href helper", () => {
    const panel = source(
      "features/admin/components/admin-stock-attention-panel.tsx",
    );
    expect(panel).toContain("adminProductDetailHref");
    expect(panel).toContain("Ver producto");
    expect(panel).toContain("ProductImage");
    expect(panel).toContain("formatAdminPresentation");
  });

  it("keeps both sections visible and uses warning vs emptied visual tones", () => {
    const panel = source(
      "features/admin/components/admin-stock-attention-panel.tsx",
    );
    expect(panel).toContain('kind="low"');
    expect(panel).toContain('kind="out"');
    expect(panel).toContain("WarningIcon");
    expect(panel).toContain("PackageIcon");
    expect(panel).toContain("sf-warning");
    expect(panel).toContain("sf-error");
  });
});
