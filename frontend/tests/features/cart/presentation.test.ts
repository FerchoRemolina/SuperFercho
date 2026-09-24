import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("cart presentation", () => {
  it("compacts CartLine and keeps remove/stock presentation secondary", () => {
    const line = source("features/cart/components/cart-line.tsx");
    expect(line).toContain("line-clamp-2");
    expect(line).toContain("priceChanged");
    expect(line).toContain("Agotado");
    expect(line).toContain("text-sf-error");
    expect(line).toContain("QuantityStepper");
    expect(line).toContain("Eliminar");
    expect(line).not.toContain("Precio al agregar:");
  });

  it("keeps checkout primary and softens clear-cart / follow shopping", () => {
    const summary = source("features/cart/components/cart-summary.tsx");
    const page = source("features/cart/components/cart-page-content.tsx");
    expect(summary).toContain("Subtotal");
    expect(summary).not.toContain("Suma de líneas");
    expect(summary).toContain("Ir a pagar");
    expect(summary).toContain("Seguir comprando");
    expect(summary).toContain('href="/catalog"');
    expect(summary).toContain("Vaciar carrito");
    expect(page).toContain("order-1 md:order-2");
    expect(page).toContain("md:sticky");
  });
});
