import { request } from "@/shared/api/client";
import type { Money } from "@/shared/money/money";

/** Mirrors CartRestResponse for GET/POST/PUT /api/v1/cart. */
export type CartStatus = "ACTIVE";

/** Mirrors CartItemRestResponse. */
export type CartItem = {
  id: string;
  productId: string;
  quantity: number;
  priceAtAddition: Money;
  addedAt: string;
  updatedAt: string;
};

export type Cart = {
  id: string;
  customerId: string;
  status: CartStatus;
  items: CartItem[];
  createdAt: string;
  updatedAt: string;
};

/** Mirrors AddItemRequest. */
export type AddCartItemRequest = {
  productId: string;
  quantity: number;
};

/** Mirrors ChangeItemQuantityRequest. */
export type ChangeCartItemQuantityRequest = {
  quantity: number;
};

export function cartKeys() {
  return {
    root: () => ["cart"] as const,
  };
}

export async function getCart(): Promise<Cart> {
  return request<Cart>("/cart");
}

export async function addProductToCart(
  body: AddCartItemRequest,
): Promise<Cart> {
  return request<Cart>("/cart/items", {
    method: "POST",
    body,
  });
}

export async function changeCartItemQuantity(
  productId: string,
  body: ChangeCartItemQuantityRequest,
): Promise<Cart> {
  return request<Cart>(`/cart/items/${encodeURIComponent(productId)}`, {
    method: "PUT",
    body,
  });
}

export async function removeCartItem(productId: string): Promise<void> {
  await request<void>(`/cart/items/${encodeURIComponent(productId)}`, {
    method: "DELETE",
  });
}

export async function clearCart(): Promise<void> {
  await request<void>("/cart", { method: "DELETE" });
}

/** Presentation helper: sum of line quantities from the cart response. */
export function cartUnitCount(cart: Cart | undefined): number {
  if (!cart) {
    return 0;
  }
  return cart.items.reduce((sum, item) => sum + item.quantity, 0);
}

/**
 * Presentation of quantity × priceAtAddition. Not a backend total and not
 * the amount charged at checkout.
 */
export function lineDisplayAmount(item: CartItem): Money {
  return scaleMoney(item.priceAtAddition, item.quantity);
}

export function cartLinesDisplayAmount(cart: Cart): Money | null {
  if (cart.items.length === 0) {
    return null;
  }
  return cart.items
    .map(lineDisplayAmount)
    .reduce((total, line) => addMoney(total, line));
}

function scaleMoney(money: Money, quantity: number): Money {
  return {
    amount: roundMoneyAmount(money.amount * quantity),
    currency: money.currency,
  };
}

function addMoney(left: Money, right: Money): Money {
  return {
    amount: roundMoneyAmount(left.amount + right.amount),
    currency: left.currency,
  };
}

function roundMoneyAmount(value: number): number {
  return Math.round(value * 100) / 100;
}
