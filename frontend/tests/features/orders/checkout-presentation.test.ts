import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("checkout presentation", () => {
  const page = source("features/orders/components/checkout-page-content.tsx");

  it("adds contextual back navigation to cart", () => {
    expect(page).toContain('href="/cart"');
    expect(page).toContain("← Carrito");
    expect(page).toContain("Confirmar pedido");
  });

  it("humanizes checkout copy and avoids internal pricing jargon", () => {
    expect(page).toContain("Subtotal");
    expect(page).toMatch(/\bTotal\b/);
    expect(page).toContain("Los precios se actualizan al confirmar tu pedido.");
    expect(page).not.toContain("precio vigente");
    expect(page).not.toContain("precio al agregar");
    expect(page).not.toContain("Total según precio vigente");
    expect(page).not.toContain("No uses el precio al agregar");
  });

  it("compacts product lines and keeps confirmation hierarchy", () => {
    expect(page).toContain("line-clamp-2");
    expect(page).toContain("Cantidad:");
    expect(page).toContain("c/u");
    expect(page).toContain("ProductImage");
    expect(page).toContain("border-sf-primary bg-sf-primary/5");
    expect(page).toContain("sticky bottom-0");
    expect(page).toContain("Confirmando…");
    expect(page).toContain("disabled={!canConfirm}");
  });

  it("does not alter checkout calculation or payment selection logic", () => {
    expect(page).toContain("buildCheckoutItems");
    expect(page).toContain("expectedCheckoutTotal");
    expect(page).toContain("useState<PaymentMethod | null>");
    expect(page).toContain("PAYMENT_METHODS.map");
  });
});
