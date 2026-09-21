import { addProductToCart, type Cart } from "@/features/cart/api";

const PENDING_ADD_KEY = "superfercho.pending-add-to-cart";

export type PendingAddToCart = {
  productId: string;
  quantity: number;
};

export function savePendingAddToCart(intent: PendingAddToCart): void {
  if (typeof window === "undefined") {
    return;
  }
  window.sessionStorage.setItem(PENDING_ADD_KEY, JSON.stringify(intent));
}

export function readPendingAddToCart(): PendingAddToCart | null {
  if (typeof window === "undefined") {
    return null;
  }
  const raw = window.sessionStorage.getItem(PENDING_ADD_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed: unknown = JSON.parse(raw);
    if (!isPendingAddToCart(parsed)) {
      window.sessionStorage.removeItem(PENDING_ADD_KEY);
      return null;
    }
    return parsed;
  } catch {
    window.sessionStorage.removeItem(PENDING_ADD_KEY);
    return null;
  }
}

export function clearPendingAddToCart(): void {
  if (typeof window === "undefined") {
    return;
  }
  window.sessionStorage.removeItem(PENDING_ADD_KEY);
}

export async function fulfillPendingAddToCart(): Promise<Cart | null> {
  const intent = readPendingAddToCart();
  if (!intent) {
    return null;
  }
  clearPendingAddToCart();
  return addProductToCart(intent);
}

function isPendingAddToCart(value: unknown): value is PendingAddToCart {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const record = value as Record<string, unknown>;
  return (
    typeof record.productId === "string" &&
    record.productId.length > 0 &&
    typeof record.quantity === "number" &&
    Number.isInteger(record.quantity) &&
    record.quantity > 0
  );
}
