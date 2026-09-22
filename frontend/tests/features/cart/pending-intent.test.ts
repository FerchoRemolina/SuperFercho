import { afterEach, describe, expect, it, vi } from "vitest";
import {
  clearPendingAddToCart,
  readPendingAddToCart,
  savePendingAddToCart,
} from "@/features/cart/pending-intent";

class MemoryStorage {
  private readonly values = new Map<string, string>();

  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value);
  }

  removeItem(key: string): void {
    this.values.delete(key);
  }
}

const storage = new MemoryStorage();

vi.stubGlobal("window", { sessionStorage: storage });

afterEach(() => {
  clearPendingAddToCart();
});

describe("pending add-to-cart intent", () => {
  it("stores and reads a purchase intent without treating it as a cart", () => {
    savePendingAddToCart({
      productId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
      quantity: 1,
    });

    expect(readPendingAddToCart()).toEqual({
      productId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
      quantity: 1,
    });

    clearPendingAddToCart();
    expect(readPendingAddToCart()).toBeNull();
  });

  it("stores the selected quantity, not always one unit", () => {
    savePendingAddToCart({
      productId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
      quantity: 3,
    });

    expect(readPendingAddToCart()).toEqual({
      productId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
      quantity: 3,
    });
  });

  it("discards invalid stored JSON", () => {
    window.sessionStorage.setItem(
      "superfercho.pending-add-to-cart",
      '{"productId":"","quantity":0}',
    );
    expect(readPendingAddToCart()).toBeNull();
  });
});
