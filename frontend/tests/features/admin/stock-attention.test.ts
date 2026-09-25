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
    expect(hub).toContain("Panel de administración");
    expect(hub).not.toContain("/admin/stock");
    expect(hub).not.toContain("brandingAssets");
    expect(hub).toContain("PackageIcon");
    expect(hub).toContain("TagIcon");
    expect(hub).toContain("ClipboardListIcon");
    expect(hub).toContain("BookIcon");
    expect(hub).toContain("HubProductsArt");
    expect(hub).toContain("Ir a productos");
    expect(panel).toContain('useAdminProductsQuery({ status: "ACTIVE" })');
    expect(panel).toContain("partitionAdminStockAttention");
    expect(panel).toContain("Control de inventario");
    expect(panel).toContain("Quedan pocas unidades");
    expect(panel).not.toContain("Próximos a agotarse");
    expect(panel).not.toContain("Activos con stock entre 1 y 5 unidades.");
    expect(panel).not.toContain("Activos con stock en cero.");
    expect(panel).toContain("Agotados");
    expect(panel).toContain("md:grid-cols-2");
    expect(panel).toContain("StockEmptyState");
    expect(panel).toContain("HubStockEmptyArt");
    expect(panel).toContain("¡Todo bajo control!");
    expect(panel).toContain("ADMIN_STOCK_LOW_EMPTY_MESSAGE");
    expect(panel).toContain("ADMIN_STOCK_OUT_EMPTY_MESSAGE");
    expect(panel).toContain("No se pudieron cargar los productos.");
    expect(panel).not.toContain("Actualizar");
    expect(ADMIN_STOCK_LOW_EMPTY_MESSAGE).toBe(
      "No hay productos que necesiten reposición en este momento.",
    );
    expect(ADMIN_STOCK_OUT_EMPTY_MESSAGE).toBe(
      "No hay productos agotados en este momento.",
    );
    expect(ADMIN_STOCK_LOW_EMPTY_MESSAGE).not.toMatch(/1 a 5/);
    expect(panel).not.toContain("Ver todos");
    expect(panel).not.toContain("adjustAdminProductStock");
    expect(panel).not.toContain("ProductStockPanel");
  });

  it("shows stock badges only for low stock and hides STOCK 0 in Agotados", () => {
    const panel = source(
      "features/admin/components/admin-stock-attention-panel.tsx",
    );
    expect(panel).toContain("Stock {product.stock}");
    expect(panel).toContain("isLow ? (");
    expect(panel).toContain("adminProductDetailHref");
    expect(panel).toContain("Ver producto");
    expect(panel).toContain("ProductImage");
    expect(panel).toContain("formatAdminPresentation");
    expect(panel).toContain("min-h-9");
    expect(panel).toContain("bg-sf-bg");
    expect(panel).not.toContain("bg-emerald-50");
  });

  it("keeps both sections visible and uses warning vs emptied visual tones", () => {
    const panel = source(
      "features/admin/components/admin-stock-attention-panel.tsx",
    );
    expect(panel).toContain('kind="low"');
    expect(panel).toContain('kind="out"');
    expect(panel).toContain("WarningIcon");
    expect(panel).toContain("PackageIcon");
    expect(panel).toContain("amber-");
    expect(panel).toContain("red-");
  });
});

describe("storefront preview enter button presentation", () => {
  it("uses indigo styling and an eye icon without changing preview behavior", () => {
    const enter = source(
      "features/admin/components/storefront-preview-enter-button.tsx",
    );
    expect(enter).toContain("enterStorefrontPreview");
    expect(enter).toContain("Ver tienda");
    expect(enter).toContain("EyeIcon");
    expect(enter).toContain("bg-indigo-700");
    expect(enter).not.toContain('variant="primary"');
    expect(enter).not.toContain("buttonClassName");
  });
});
