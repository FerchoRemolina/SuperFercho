import { afterEach, describe, expect, it, vi } from "vitest";
import {
  addFavorite,
  favoritesKeys,
  getFavorites,
  isFavoriteProductPurchasable,
  isFavoriteProductUnavailable,
  isProductInFavorites,
  removeFavorite,
  type Favorite,
  type FavoriteItem,
  type FavoriteList,
  type FavoriteProduct,
} from "@/features/favorites/api";
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

const productId = "cccccccc-cccc-cccc-cccc-cccccccccccc";
const customerId = "11111111-1111-1111-1111-111111111111";

const availableProduct: FavoriteProduct = {
  id: productId,
  name: "Leche entera",
  brand: "Colanta",
  price: { amount: 4500, currency: "COP" },
  imageUrl: "https://cdn.example/leche.png",
  categoryId: "dddddddd-dddd-dddd-dddd-dddddddddddd",
  status: "ACTIVE",
  available: true,
};

const favoriteList: FavoriteList = {
  items: [
    {
      productId,
      createdAt: "2026-09-21T12:00:00Z",
      product: availableProduct,
    },
  ],
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

describe("favorites api", () => {
  it("sends the JWT on GET /favorites and never sends customerId or userId", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: customerId,
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(favoriteList));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getFavorites()).resolves.toEqual(favoriteList);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/favorites");
    expect(url).not.toContain("customerId");
    expect(url).not.toContain("userId");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer access-token",
    );
    expect(init.body).toBeUndefined();
    expect(new Headers(init.headers).get("Content-Type")).toBeNull();
  });

  it("maps product composition including status and available", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(favoriteList));
    vi.stubGlobal("fetch", fetchMock);

    const result = await getFavorites();
    const product = result.items[0]?.product;

    expect(product).toEqual(availableProduct);
    expect(product?.status).toBe("ACTIVE");
    expect(product?.available).toBe(true);
  });

  it("keeps product null without refetching the catalog", async () => {
    const listWithMissingProduct: FavoriteList = {
      items: [
        {
          productId,
          createdAt: "2026-09-21T12:00:00Z",
          product: null,
        },
      ],
    };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(listWithMissingProduct));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getFavorites()).resolves.toEqual(listWithMissingProduct);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/favorites",
    );
  });

  it("preserves INACTIVE status and available false", async () => {
    const inactive: FavoriteProduct = {
      ...availableProduct,
      status: "INACTIVE",
      available: false,
    };
    const list: FavoriteList = {
      items: [
        {
          productId,
          createdAt: "2026-09-21T12:00:00Z",
          product: inactive,
        },
      ],
    };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(list));
    vi.stubGlobal("fetch", fetchMock);

    const result = await getFavorites();
    expect(result.items[0]?.product?.status).toBe("INACTIVE");
    expect(result.items[0]?.product?.available).toBe(false);
  });

  it("posts /favorites/{productId} without a body and accepts 201", async () => {
    const created: Favorite = {
      id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      productId,
      createdAt: "2026-09-21T12:00:00Z",
    };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(created, 201));
    vi.stubGlobal("fetch", fetchMock);

    await expect(addFavorite(productId)).resolves.toEqual(created);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(`http://localhost:8080/api/v1/favorites/${productId}`);
    expect(init.method).toBe("POST");
    expect(init.body).toBeUndefined();
    expect(new Headers(init.headers).get("Content-Type")).toBeNull();
  });

  it("posts /favorites/{productId} without a body and accepts 200", async () => {
    const existing: Favorite = {
      id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      productId,
      createdAt: "2026-09-21T11:00:00Z",
    };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(existing, 200));
    vi.stubGlobal("fetch", fetchMock);

    await expect(addFavorite(productId)).resolves.toEqual(existing);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(`http://localhost:8080/api/v1/favorites/${productId}`);
    expect(init.method).toBe("POST");
    expect(init.body).toBeUndefined();
  });

  it("deletes /favorites/{productId} without a body and accepts 204", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(removeFavorite(productId)).resolves.toBeUndefined();

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(`http://localhost:8080/api/v1/favorites/${productId}`);
    expect(init.method).toBe("DELETE");
    expect(init.body).toBeUndefined();
    expect(new Headers(init.headers).get("Content-Type")).toBeNull();
  });

  it("uses a single query key for favorites", () => {
    expect(favoritesKeys().root()).toEqual(["favorites"]);
  });
});

describe("favorites helpers", () => {
  const items: FavoriteItem[] = [
    {
      productId,
      createdAt: "2026-09-21T12:00:00Z",
      product: null,
    },
  ];

  it("derives membership from productId even when product is null", () => {
    expect(isProductInFavorites(items, productId)).toBe(true);
    expect(isProductInFavorites(items, "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")).toBe(
      false,
    );
    expect(isProductInFavorites(undefined, productId)).toBe(false);
  });

  it("treats an existing available product as purchasable", () => {
    expect(isFavoriteProductPurchasable(availableProduct)).toBe(true);
    expect(isFavoriteProductUnavailable(availableProduct)).toBe(false);
  });

  it("treats an INACTIVE product as unavailable", () => {
    const inactive: FavoriteProduct = {
      ...availableProduct,
      status: "INACTIVE",
      available: false,
    };
    expect(isFavoriteProductPurchasable(inactive)).toBe(false);
    expect(isFavoriteProductUnavailable(inactive)).toBe(true);
  });

  it("treats available=false as unavailable even if status is ACTIVE", () => {
    const notSellable: FavoriteProduct = {
      ...availableProduct,
      status: "ACTIVE",
      available: false,
    };
    expect(isFavoriteProductPurchasable(notSellable)).toBe(false);
    expect(isFavoriteProductUnavailable(notSellable)).toBe(true);
  });

  it("treats a null product as unavailable", () => {
    expect(isFavoriteProductPurchasable(null)).toBe(false);
    expect(isFavoriteProductUnavailable(null)).toBe(true);
  });
});
