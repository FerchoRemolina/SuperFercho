import { request } from "@/shared/api/client";

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
): Promise<ShoppingList> {
  return request<ShoppingList>(
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
