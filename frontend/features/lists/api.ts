import { request } from "@/shared/api/client";
import type { Cart } from "@/features/cart/api";

/** Mirrors ShoppingListItemRestResponse. */
export type ShoppingListItem = {
  id: string;
  productId: string;
  quantity: number;
  createdAt: string;
};

/** Mirrors ShoppingListRestResponse. `customerId` is response-only; never sent by the client. */
export type ShoppingList = {
  id: string;
  customerId: string;
  name: string;
  items: ShoppingListItem[];
  createdAt: string;
  updatedAt: string;
};

/** Mirrors CreateShoppingListRequest / RenameShoppingListRequest. */
export type ShoppingListNameRequest = {
  name: string;
};

/** Mirrors AddItemRequest. */
export type AddShoppingListItemRequest = {
  productId: string;
  quantity: number;
};

/** Mirrors AddShoppingListItemRestResponse. */
export type AddShoppingListItemResult = {
  shoppingList: ShoppingList;
  alreadyPresent: boolean;
};

/** Mirrors ChangeItemQuantityRequest. */
export type ChangeShoppingListItemQuantityRequest = {
  quantity: number;
};

export function shoppingListsKeys() {
  return {
    root: () => ["shopping-lists"] as const,
    detail: (shoppingListId: string) =>
      ["shopping-lists", "detail", shoppingListId] as const,
  };
}

export async function listShoppingLists(): Promise<ShoppingList[]> {
  return request<ShoppingList[]>("/shopping-lists");
}

export async function getShoppingList(
  shoppingListId: string,
): Promise<ShoppingList> {
  return request<ShoppingList>(
    `/shopping-lists/${encodeURIComponent(shoppingListId)}`,
  );
}

export async function createShoppingList(
  body: ShoppingListNameRequest,
): Promise<ShoppingList> {
  return request<ShoppingList>("/shopping-lists", {
    method: "POST",
    body,
  });
}

export async function renameShoppingList(
  shoppingListId: string,
  body: ShoppingListNameRequest,
): Promise<ShoppingList> {
  return request<ShoppingList>(
    `/shopping-lists/${encodeURIComponent(shoppingListId)}`,
    {
      method: "PATCH",
      body,
    },
  );
}

export async function addShoppingListItem(
  shoppingListId: string,
  body: AddShoppingListItemRequest,
): Promise<AddShoppingListItemResult> {
  return request<AddShoppingListItemResult>(
    `/shopping-lists/${encodeURIComponent(shoppingListId)}/items`,
    {
      method: "POST",
      body,
    },
  );
}

export async function changeShoppingListItemQuantity(
  shoppingListId: string,
  productId: string,
  body: ChangeShoppingListItemQuantityRequest,
): Promise<ShoppingList> {
  return request<ShoppingList>(
    `/shopping-lists/${encodeURIComponent(shoppingListId)}/items/${encodeURIComponent(productId)}`,
    {
      method: "PATCH",
      body,
    },
  );
}

export async function removeShoppingListItem(
  shoppingListId: string,
  productId: string,
): Promise<void> {
  await request<void>(
    `/shopping-lists/${encodeURIComponent(shoppingListId)}/items/${encodeURIComponent(productId)}`,
    { method: "DELETE" },
  );
}

export async function clearShoppingList(
  shoppingListId: string,
): Promise<void> {
  await request<void>(
    `/shopping-lists/${encodeURIComponent(shoppingListId)}/items`,
    { method: "DELETE" },
  );
}

/**
 * Compra de la lista: una sola llamada al endpoint oficial del backend, que
 * agrega al carrito únicamente los productos con cantidad mayor a 0. La
 * respuesta es el carrito resultante.
 */
export async function addShoppingListToCart(
  shoppingListId: string,
): Promise<Cart> {
  return request<Cart>(
    `/shopping-lists/${encodeURIComponent(shoppingListId)}/add-to-cart`,
    { method: "POST" },
  );
}

export async function deleteShoppingList(
  shoppingListId: string,
): Promise<void> {
  await request<void>(
    `/shopping-lists/${encodeURIComponent(shoppingListId)}`,
    { method: "DELETE" },
  );
}

export function shoppingListItemCount(list: ShoppingList): number {
  return list.items.length;
}

/** Pertenencia producto-lista: añadir es idempotente, la cantidad no crece. */
export function shoppingListContainsProduct(
  list: ShoppingList,
  productId: string,
): boolean {
  return list.items.some((item) => item.productId === productId);
}

/** Resumen de la compra: productos con cantidad mayor a 0 y sus unidades. */
export type ShoppingListPurchaseSummary = {
  itemCount: number;
  unitCount: number;
};

export function shoppingListPurchaseSummary(
  list: ShoppingList,
): ShoppingListPurchaseSummary {
  const buyable = list.items.filter((item) => item.quantity > 0);
  return {
    itemCount: buyable.length,
    unitCount: buyable.reduce((sum, item) => sum + item.quantity, 0),
  };
}

export function shoppingListUnitCount(list: ShoppingList): number {
  return list.items.reduce((sum, item) => sum + item.quantity, 0);
}

/** Client validation aligned to CreateShoppingListRequest / RenameShoppingListRequest. */
export function validateShoppingListName(name: string): string | undefined {
  const trimmed = name.trim();
  if (trimmed.length === 0) {
    return "Escribe un nombre para la lista.";
  }
  if (trimmed.length > 255) {
    return "El nombre no puede superar 255 caracteres.";
  }
  return undefined;
}
