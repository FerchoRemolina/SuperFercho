import { afterEach, describe, expect, it, vi } from "vitest";
import {
  getCategory,
  getProduct,
  listCategories,
  listProducts,
  searchProducts,
} from "@/features/catalog/api";
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

const product = {
  id: "33333333-3333-3333-3333-333333333333",
  categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  barcode: "7701234567890",
  name: "Leche entera",
  brand: "Alquería",
  description: "Bolsa 1L",
  price: { amount: 10.5, currency: "COP" },
  stock: 20,
  imageUrl: "https://img.test/leche.png",
  status: "ACTIVE",
  createdAt: "2026-03-01T10:00:00Z",
  updatedAt: "2026-03-01T10:30:00Z",
};

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  clearSession();
  configureSessionPersistence(null);
});

describe("catalog api", () => {
  it("lists public categories without sending a JWT", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await listCategories();

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/categories");
    expect(new Headers(init.headers).get("Authorization")).toBeNull();
  });

  it("lists products with categoryId when provided", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify([product]), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await listProducts({
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    });

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/products?categoryId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    );
    expect(result).toEqual([product]);
  });

  it("searches products with the backend text parameter", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify([product]), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await searchProducts({ text: "leche" });

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/products/search?text=leche",
    );
  });

  it("gets a product and a category by id", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify(product), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            name: "Lácteos",
            description: null,
            status: "ACTIVE",
            createdAt: "2026-03-01T10:00:00Z",
            updatedAt: "2026-03-01T10:00:00Z",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(getProduct(product.id)).resolves.toEqual(product);
    await expect(getCategory(product.categoryId)).resolves.toMatchObject({
      name: "Lácteos",
    });
    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/products/${product.id}`,
    );
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      `http://localhost:8080/api/v1/categories/${product.categoryId}`,
    );
  });
});
