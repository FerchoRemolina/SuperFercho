import type { CartItem } from "@/features/cart/api";
import {
  getProduct,
  listProducts,
  type Product,
} from "@/features/catalog/api";
import type { CheckoutItemRequest } from "@/features/orders/api";
import type { Money } from "@/shared/money/money";

export function productsById(products: Product[]): Map<string, Product> {
  return new Map(products.map((product) => [product.id, product]));
}

/**
 * Builds CheckoutRequest items from the cart using catalog current prices.
 * Returns null when any line lacks a catalog product or a valid quantity.
 * Does not use priceAtAddition.
 */
export function buildCheckoutItems(
  items: CartItem[],
  catalog: Map<string, Product>,
): CheckoutItemRequest[] | null {
  if (items.length === 0) {
    return null;
  }

  const result: CheckoutItemRequest[] = [];
  for (const item of items) {
    const product = catalog.get(item.productId);
    if (!product || item.quantity < 1) {
      return null;
    }
    result.push({
      productId: item.productId,
      quantity: item.quantity,
      expectedUnitPrice: {
        amount: product.price.amount,
        currency: product.price.currency,
      },
    });
  }
  return result;
}

export function expectedCheckoutTotal(
  items: CheckoutItemRequest[],
): Money | null {
  if (items.length === 0) {
    return null;
  }
  return items
    .map((item) => scaleMoney(item.expectedUnitPrice, item.quantity))
    .reduce((total, line) => addMoney(total, line));
}

export async function loadCatalogProductsForCart(
  items: CartItem[],
): Promise<Product[]> {
  const listed = await listProducts();
  const byId = productsById(listed);
  const loaded = [...listed];

  for (const item of items) {
    if (byId.has(item.productId)) {
      continue;
    }
    const product = await getProduct(item.productId);
    byId.set(product.id, product);
    loaded.push(product);
  }

  return loaded;
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
