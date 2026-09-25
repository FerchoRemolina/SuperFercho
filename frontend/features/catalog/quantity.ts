/** UX helpers over catalog stock. Checkout remains the inventory authority. */

import type { ProductStatus } from "@/features/catalog/api";

/**
 * Availability label from status + stock (approved matrix).
 * ACTIVE+stock>0 → Disponible; ACTIVE+0 → Agotado; INACTIVE → No disponible; ARCHIVED → Archivado.
 */
export function productAvailabilityLabel(
  status: ProductStatus,
  stock: number,
): string {
  if (status === "ARCHIVED") {
    return "Archivado";
  }
  if (status === "INACTIVE") {
    return "No disponible";
  }
  return stock > 0 ? "Disponible" : "Agotado";
}

export function productStockLabel(stock: number): string {
  if (stock <= 0) {
    return "Agotado";
  }
  if (stock === 1) {
    return "1 unidad disponible";
  }
  return `${stock} unidades disponibles`;
}

/**
 * Sellability (`available` / ACTIVE) and stock are independent.
 * Hide the cart CTA only when stock is known and <= 0; unknown stock does not block the CTA.
 */
export function canOfferAddToCart(
  sellable: boolean,
  stock: number | undefined,
): boolean {
  if (!sellable) {
    return false;
  }
  if (stock !== undefined && stock <= 0) {
    return false;
  }
  return true;
}

export function initialSelectedQuantity(stock: number): number {
  return stock > 0 ? 1 : 0;
}

export function clampSelectedQuantity(quantity: number, stock: number): number {
  if (stock <= 0) {
    return 0;
  }
  if (!Number.isInteger(quantity) || quantity < 1) {
    return 1;
  }
  return Math.min(quantity, stock);
}

export function canDecrementSelectedQuantity(quantity: number): boolean {
  return quantity > 1;
}

export function canIncrementSelectedQuantity(quantity: number, stock: number): boolean {
  return stock > 0 && quantity < stock;
}

export function nextSelectedQuantity(
  quantity: number,
  delta: -1 | 1,
  stock: number,
): number {
  return clampSelectedQuantity(quantity + delta, stock);
}

export function quantityToAddToCart(
  quantity: number,
  stock: number | undefined,
): number | null {
  if (stock !== undefined && stock <= 0) {
    return null;
  }
  if (stock === undefined) {
    return Number.isInteger(quantity) && quantity >= 1 ? quantity : null;
  }
  const clamped = clampSelectedQuantity(quantity, stock);
  return clamped >= 1 ? clamped : null;
}
