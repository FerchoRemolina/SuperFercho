import { describe, expect, it } from "vitest";
import {
  canIncreaseCartQuantity,
  cartStockWarning,
  isCartQuantityAboveStock,
} from "@/features/cart/stock";

describe("cart stock UX", () => {
  it("allows increasing when catalog stock is unknown", () => {
    expect(canIncreaseCartQuantity(9, undefined)).toBe(true);
    expect(isCartQuantityAboveStock(9, undefined)).toBe(false);
    expect(cartStockWarning(9, undefined)).toBeNull();
  });

  it("stops increasing at the known catalog stock", () => {
    expect(canIncreaseCartQuantity(2, 3)).toBe(true);
    expect(canIncreaseCartQuantity(3, 3)).toBe(false);
    expect(canIncreaseCartQuantity(4, 3)).toBe(false);
  });

  it("warns when the cart quantity exceeds current stock", () => {
    expect(isCartQuantityAboveStock(4, 2)).toBe(true);
    expect(cartStockWarning(4, 2)).toBe(
      "Solo hay 2 unidades disponibles. Reduce la cantidad para continuar.",
    );
    expect(cartStockWarning(2, 2)).toBeNull();
  });

  it("warns when stock drops to zero while the line remains in the cart", () => {
    expect(isCartQuantityAboveStock(1, 0)).toBe(true);
    expect(cartStockWarning(1, 0)).toBe(
      "Este producto ya no tiene unidades disponibles. Quita la línea o espera a que haya existencias.",
    );
    expect(canIncreaseCartQuantity(1, 0)).toBe(false);
  });
});
