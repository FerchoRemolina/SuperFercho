import { describe, expect, it } from "vitest";
import { isProductAvailable } from "@/features/catalog/api";
import {
  canDecrementSelectedQuantity,
  canIncrementSelectedQuantity,
  clampSelectedQuantity,
  initialSelectedQuantity,
  nextSelectedQuantity,
  productStockLabel,
  quantityToAddToCart,
} from "@/features/catalog/quantity";

const product = {
  id: "33333333-3333-3333-3333-333333333333",
  categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  barcode: null,
  name: "Leche",
  brand: null,
  description: null,
  price: { amount: 4500, currency: "COP" as const },
  stock: 8,
  imageUrl: null,
  status: "ACTIVE" as const,
  createdAt: "2026-09-21T12:00:00Z",
  updatedAt: "2026-09-21T12:00:00Z",
};

describe("catalog stock UX", () => {
  it("starts quantity at 1 when there is stock", () => {
    expect(initialSelectedQuantity(8)).toBe(1);
    expect(initialSelectedQuantity(1)).toBe(1);
  });

  it("increments until stock and does not go past it", () => {
    expect(nextSelectedQuantity(1, 1, 3)).toBe(2);
    expect(nextSelectedQuantity(2, 1, 3)).toBe(3);
    expect(nextSelectedQuantity(3, 1, 3)).toBe(3);
    expect(canIncrementSelectedQuantity(3, 3)).toBe(false);
  });

  it("decrements down to 1 and no lower", () => {
    expect(nextSelectedQuantity(3, -1, 8)).toBe(2);
    expect(nextSelectedQuantity(1, -1, 8)).toBe(1);
    expect(canDecrementSelectedQuantity(1)).toBe(false);
    expect(canDecrementSelectedQuantity(2)).toBe(true);
  });

  it("sends the selected quantity and never a sold-out add", () => {
    expect(quantityToAddToCart(3, 8)).toBe(3);
    expect(quantityToAddToCart(9, 8)).toBe(8);
    expect(quantityToAddToCart(1, 0)).toBeNull();
    expect(quantityToAddToCart(2, undefined)).toBe(2);
  });

  it("labels stock 0 as Agotado and hides purchase availability", () => {
    expect(productStockLabel(0)).toBe("Agotado");
    expect(isProductAvailable({ ...product, stock: 0 })).toBe(false);
    expect(initialSelectedQuantity(0)).toBe(0);
    expect(clampSelectedQuantity(4, 0)).toBe(0);
  });

  it("labels remaining units when stock is positive", () => {
    expect(productStockLabel(1)).toBe("1 unidad disponible");
    expect(productStockLabel(8)).toBe("8 unidades disponibles");
    expect(isProductAvailable(product)).toBe(true);
  });
});
