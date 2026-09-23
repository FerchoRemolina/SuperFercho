import { afterEach, describe, expect, it, vi } from "vitest";
import {
  addShoppingListItem,
  changeShoppingListItemQuantity,
  clearShoppingList,
  createShoppingList,
  getShoppingList,
  listShoppingLists,
  removeShoppingListItem,
  renameShoppingList,
  shoppingListItemCount,
  shoppingListsKeys,
  shoppingListUnitCount,
  validateShoppingListName,
  type ShoppingList,
} from "@/features/lists/api";
import {
  formatListInstant,
  shoppingListHref,
  shoppingListItemsLabel,
} from "@/features/lists/presentation";
import {
  clearSession,
  configureSessionPersistence,
  setSession,
  type SessionPersistence,
} from "@/shared/session/session";

class MemoryPersistence implements SessionPersistence {
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

const listId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
const productId = "cccccccc-cccc-cccc-cccc-cccccccccccc";
const customerId = "11111111-1111-1111-1111-111111111111";

const shoppingList: ShoppingList = {
  id: listId,
  customerId,
  name: "Mercado semanal",
  items: [
    {
      id: "99999999-9999-9999-9999-000000000001",
      productId,
      quantity: 2,
      createdAt: "2026-04-01T10:00:00Z",
    },
  ],
  createdAt: "2026-04-01T10:00:00Z",
  updatedAt: "2026-04-01T10:05:00Z",
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  clearSession();
  configureSessionPersistence(null);
});

describe("shopping lists api", () => {
  it("sends the JWT on GET /shopping-lists and never sends customerId", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: customerId,
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([shoppingList]));
    vi.stubGlobal("fetch", fetchMock);

    await expect(listShoppingLists()).resolves.toEqual([shoppingList]);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/shopping-lists");
    expect(url).not.toContain("customerId");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer access-token",
    );
    expect(init.body).toBeUndefined();
  });

  it("gets a shopping list detail by id", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(shoppingList));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getShoppingList(listId)).resolves.toEqual(shoppingList);

    const [url] = fetchMock.mock.calls[0] as [string];
    expect(url).toBe(`http://localhost:8080/api/v1/shopping-lists/${listId}`);
  });

  it("creates a shopping list with name only", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(shoppingList, 201));
    vi.stubGlobal("fetch", fetchMock);

    await expect(createShoppingList({ name: "Mercado semanal" })).resolves.toEqual(
      shoppingList,
    );

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/shopping-lists");
    expect(init.method).toBe("POST");
    expect(init.body).toBe(JSON.stringify({ name: "Mercado semanal" }));
    expect(init.body).not.toContain("customerId");
  });

  it("renames a shopping list with PATCH", async () => {
    const renamed = { ...shoppingList, name: "Fin de semana" };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(renamed));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      renameShoppingList(listId, { name: "Fin de semana" }),
    ).resolves.toEqual(renamed);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(`http://localhost:8080/api/v1/shopping-lists/${listId}`);
    expect(init.method).toBe("PATCH");
    expect(init.body).toBe(JSON.stringify({ name: "Fin de semana" }));
  });

  it("adds an item with productId and quantity", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(shoppingList));
    vi.stubGlobal("fetch", fetchMock);

    await addShoppingListItem(listId, { productId, quantity: 2 });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      `http://localhost:8080/api/v1/shopping-lists/${listId}/items`,
    );
    expect(init.method).toBe("POST");
    expect(init.body).toBe(JSON.stringify({ productId, quantity: 2 }));
  });

  it("changes item quantity with PATCH", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(shoppingList));
    vi.stubGlobal("fetch", fetchMock);

    await changeShoppingListItemQuantity(listId, productId, { quantity: 5 });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      `http://localhost:8080/api/v1/shopping-lists/${listId}/items/${productId}`,
    );
    expect(init.method).toBe("PATCH");
    expect(init.body).toBe(JSON.stringify({ quantity: 5 }));
  });

  it("removes an item with DELETE", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(removeShoppingListItem(listId, productId)).resolves.toBeUndefined();

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      `http://localhost:8080/api/v1/shopping-lists/${listId}/items/${productId}`,
    );
    expect(init.method).toBe("DELETE");
  });

  it("clears all items with DELETE .../items", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(clearShoppingList(listId)).resolves.toBeUndefined();

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      `http://localhost:8080/api/v1/shopping-lists/${listId}/items`,
    );
    expect(init.method).toBe("DELETE");
  });

  it("never invents a list-to-cart endpoint", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse([shoppingList]))
      .mockResolvedValueOnce(jsonResponse(shoppingList));
    vi.stubGlobal("fetch", fetchMock);

    await listShoppingLists();
    await addShoppingListItem(listId, { productId, quantity: 1 });

    for (const call of fetchMock.mock.calls) {
      const url = call[0] as string;
      expect(url).not.toContain("/cart");
      expect(url).not.toContain("from-list");
    }
  });

  it("exposes stable query keys", () => {
    expect(shoppingListsKeys().root()).toEqual(["shopping-lists"]);
    expect(shoppingListsKeys().detail(listId)).toEqual([
      "shopping-lists",
      "detail",
      listId,
    ]);
  });

  it("counts items and units", () => {
    expect(shoppingListItemCount(shoppingList)).toBe(1);
    expect(shoppingListUnitCount(shoppingList)).toBe(2);
    expect(shoppingListItemCount({ ...shoppingList, items: [] })).toBe(0);
  });
});

describe("validateShoppingListName", () => {
  it("rejects blank names", () => {
    expect(validateShoppingListName("")).toBe("Escribe un nombre para la lista.");
    expect(validateShoppingListName("   ")).toBe(
      "Escribe un nombre para la lista.",
    );
  });

  it("rejects names longer than 255 characters", () => {
    expect(validateShoppingListName("a".repeat(256))).toBe(
      "El nombre no puede superar 255 caracteres.",
    );
  });

  it("accepts a valid name", () => {
    expect(validateShoppingListName(" Mercado ")).toBeUndefined();
  });
});

describe("shopping lists presentation", () => {
  it("formats labels and hrefs", () => {
    expect(shoppingListItemsLabel(0)).toBe("0 productos");
    expect(shoppingListItemsLabel(1)).toBe("1 producto");
    expect(shoppingListItemsLabel(3)).toBe("3 productos");
    expect(shoppingListHref(listId)).toBe(`/lists/${listId}`);
    expect(formatListInstant("2026-04-01T15:00:00Z")).toMatch(/2026/);
  });
});
