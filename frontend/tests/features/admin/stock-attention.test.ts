import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  ADMIN_STOCK_LOW_EMPTY_MESSAGE,
  ADMIN_STOCK_OUT_EMPTY_MESSAGE,
  aggregateRecentlySoldProducts,
  formatRecentlySoldProductLabel,
  withDisambiguatedRecentlySoldLabels,
} from "@/features/admin/presentation";
import type { AdminProduct } from "@/features/admin/api";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("admin stock attention panel", () => {
  it("keeps stock panel composition and hub inventory attention block", () => {
    const hub = source("features/admin/components/admin-hub.tsx");
    const panel = source(
      "features/admin/components/admin-stock-attention-panel.tsx",
    );

    expect(hub).toContain("InventoryAttentionHero");
    expect(hub).toContain("Inventario requiere atención");
    expect(hub).toContain("Últimos pedidos");
    expect(hub).toContain("Productos más vendidos recientemente");
    expect(hub).toContain("Ventas");
    expect(hub).toContain("SalesPeriodCard");
    expect(hub).toContain("useAdminSalesPeriodSummaryQuery");
    expect(hub).toContain("Clientes con compras recientes");
    expect(hub).toContain("useAdminRecentBuyersQuery");
    expect(hub).toContain("aggregateRecentlySoldProducts");
    expect(hub).toContain("withDisambiguatedRecentlySoldLabels");
    expect(hub).toContain("getAdminProduct");
    expect(hub).toContain("getAdminProductVariant");
    expect(hub.indexOf("RecentOrdersCard")).toBeLessThan(
      hub.indexOf("SalesPeriodCard"),
    );
    expect(hub.indexOf("SalesPeriodCard")).toBeLessThan(
      hub.indexOf("RecentBuyersCard"),
    );
    expect(hub).not.toContain("Ir a productos");
    expect(hub).not.toContain("HubProductsArt");
    expect(hub).not.toContain("FerchoAttentionCard");
    expect(hub).not.toContain("useAdminKnowledgeDocumentsQuery");
    expect(hub).not.toContain("documentos por procesar");
    expect(hub).not.toContain("Fercho —");
    expect(panel).toContain('useAdminProductsQuery({ status: "ACTIVE" })');
    expect(panel).toContain("partitionAdminStockAttention");
    expect(panel).toContain("Control de inventario");
    expect(panel).toContain("Quedan pocas unidades");
    expect(panel).toContain("Agotados");
    expect(panel).toContain("md:grid-cols-2");
    expect(ADMIN_STOCK_LOW_EMPTY_MESSAGE).toBe(
      "No hay productos que necesiten reposición en este momento.",
    );
    expect(ADMIN_STOCK_OUT_EMPTY_MESSAGE).toBe(
      "No hay productos agotados en este momento.",
    );
  });

  it("shows stock badges only for low stock and hides STOCK 0 in Agotados", () => {
    const panel = source(
      "features/admin/components/admin-stock-attention-panel.tsx",
    );
    expect(panel).toContain("Stock {product.stock}");
    expect(panel).toContain("isLow ? (");
    expect(panel).toContain("adminProductDetailHref");
    expect(panel).toContain("Ver producto");
  });

  it("keeps both sections visible and uses warning vs emptied visual tones", () => {
    const panel = source(
      "features/admin/components/admin-stock-attention-panel.tsx",
    );
    expect(panel).toContain('kind="low"');
    expect(panel).toContain('kind="out"');
    expect(panel).toContain("WarningIcon");
    expect(panel).toContain("amber-");
    expect(panel).toContain("red-");
  });
});

describe("recently sold aggregation", () => {
  it("sums quantities from the provided order sample only", () => {
    const rows = aggregateRecentlySoldProducts(
      [
        {
          items: [
            { productId: "a", productName: "Leche", quantity: 2 },
            { productId: "b", productName: "Pan", quantity: 1 },
          ],
        },
        {
          items: [{ productId: "a", productName: "Leche", quantity: 3 }],
        },
      ],
      5,
    );
    expect(rows[0]).toEqual({
      productId: "a",
      productName: "Leche",
      quantity: 5,
    });
    expect(rows[1]?.productId).toBe("b");
  });

  it("disambiguates same-name products with brand, variant and presentation", () => {
    expect(
      formatRecentlySoldProductLabel({
        name: "Gaseosa",
        brand: "Postobón",
        variantName: "Manzana",
        presentation: { quantity: 1.5, unit: "L" },
      }),
    ).toBe("Gaseosa · Postobón · Manzana · 1.5 L");

    const catalog = new Map<string, AdminProduct>([
      [
        "p1",
        {
          id: "p1",
          categoryId: "c1",
          productTypeId: "t1",
          productVariantId: "v1",
          presentation: { quantity: 1.5, unit: "L" },
          barcode: null,
          name: "Gaseosa",
          brand: "Postobón",
          description: null,
          price: { amount: 1000, currency: "COP" },
          stock: 10,
          imageUrl: null,
          status: "ACTIVE",
          createdAt: "2026-01-01T00:00:00Z",
          updatedAt: "2026-01-01T00:00:00Z",
        },
      ],
      [
        "p2",
        {
          id: "p2",
          categoryId: "c1",
          productTypeId: "t1",
          productVariantId: "v2",
          presentation: { quantity: 1.5, unit: "L" },
          barcode: null,
          name: "Gaseosa",
          brand: "Postobón",
          description: null,
          price: { amount: 1000, currency: "COP" },
          stock: 8,
          imageUrl: null,
          status: "ACTIVE",
          createdAt: "2026-01-01T00:00:00Z",
          updatedAt: "2026-01-01T00:00:00Z",
        },
      ],
    ]);
    const variants = new Map([
      ["v1", "Manzana"],
      ["v2", "Colombiana"],
    ]);
    const labeled = withDisambiguatedRecentlySoldLabels(
      [
        { productId: "p1", productName: "Gaseosa", quantity: 4 },
        { productId: "p2", productName: "Gaseosa", quantity: 3 },
      ],
      catalog,
      variants,
    );
    expect(labeled[0]?.productName).toBe(
      "Gaseosa · Postobón · Manzana · 1.5 L",
    );
    expect(labeled[1]?.productName).toBe(
      "Gaseosa · Postobón · Colombiana · 1.5 L",
    );
  });
});

describe("storefront preview enter button presentation", () => {
  it("uses SuperFercho surface styling and an eye icon without changing preview behavior", () => {
    const enter = source(
      "features/admin/components/storefront-preview-enter-button.tsx",
    );
    expect(enter).toContain("enterStorefrontPreview");
    expect(enter).toContain("Ver tienda");
    expect(enter).toContain("EyeIcon");
    expect(enter).toContain("bg-sf-primary/5");
    expect(enter).not.toContain("bg-indigo-700");
  });
});
