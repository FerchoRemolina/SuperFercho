import type { CheckoutItemRequest, PaymentMethod } from "@/features/orders/api";

export type CheckoutAttempt = {
  fingerprint: string;
  key: string;
};

export function newIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

/**
 * Stable fingerprint of one logical checkout. Matches the backend idea:
 * address + payment method + sorted lines of productId:qty:amount:currency.
 */
export function checkoutFingerprint(input: {
  addressId: string;
  paymentMethod: PaymentMethod;
  items: CheckoutItemRequest[];
}): string {
  const lines = [...input.items]
    .sort((left, right) => left.productId.localeCompare(right.productId))
    .map(
      (item) =>
        `${item.productId}:${item.quantity}:${item.expectedUnitPrice.amount}:${item.expectedUnitPrice.currency}`,
    )
    .join(",");
  return `${input.addressId}|${input.paymentMethod}|${lines}`;
}

export function keyForAttempt(
  previous: CheckoutAttempt | null,
  fingerprint: string,
): CheckoutAttempt {
  if (previous && previous.fingerprint === fingerprint) {
    return previous;
  }
  return { fingerprint, key: newIdempotencyKey() };
}
