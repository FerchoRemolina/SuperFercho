import { afterEach, describe, expect, it, vi } from "vitest";
import {
  addProductToCart,
  cartKeys,
  cartLinesDisplayAmount,
  cartUnitCount,
  changeCartItemQuantity,
  clearCart,
  getCart,
  lineDisplayAmount,
  removeCartItem,
  type Cart,
} from "@/features/cart/api";
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

const cart: Cart = {
  id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  customerId: "11111111-1111-1111-1111-111111111111",
  status: "ACTIVE",
  items: [
    {
      id: "99999999-9999-9999-9999-000000000001",
      productId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
      quantity: 2,
      priceAtAddition: { amount: 10.5, currency: "COP" },
      addedAt: "2026-04-01T10:00:00Z",
      updatedAt: "2026-04-01T10:05:00Z",
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

describe("cart api", () => {
  it("sends the JWT on GET /cart", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(cart));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getCart()).resolves.toEqual(cart);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/cart");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer access-token",
    );
  });

  it("posts productId and quantity to add an item", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(cart));
    vi.stubGlobal("fetch", fetchMock);

    await addProductToCart({
      productId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
      quantity: 2,
    });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/cart/items");
    expect(init.method).toBe("POST");
    expect(init.body).toBe(
      JSON.stringify({
        productId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
        quantity: 2,
      }),
    );
  });

  it("puts quantity on the product line", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(cart));
    vi.stubGlobal("fetch", fetchMock);

    await changeCartItemQuantity("cccccccc-cccc-cccc-cccc-cccccccccccc", {
      quantity: 3,
    });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      "http://localhost:8080/api/v1/cart/items/cccccccc-cccc-cccc-cccc-cccccccccccc",
    );
    expect(init.method).toBe("PUT");
    expect(init.body).toBe(JSON.stringify({ quantity: 3 }));
  });

  it("deletes a line and the whole cart with 204", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await removeCartItem("cccccccc-cccc-cccc-cccc-cccccccccccc");
    await clearCart();

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/cart/items/cccccccc-cccc-cccc-cccc-cccccccccccc",
    );
    expect((fetchMock.mock.calls[0]?.[1] as RequestInit).method).toBe("DELETE");
    expect(fetchMock.mock.calls[1]?.[0]).toBe("http://localhost:8080/api/v1/cart");
    expect((fetchMock.mock.calls[1]?.[1] as RequestInit).method).toBe("DELETE");
  });

  it("uses a single query key for the cart", () => {
    expect(cartKeys().root()).toEqual(["cart"]);
  });

  it("counts units and presents line amounts from priceAtAddition", () => {
    expect(cartUnitCount(cart)).toBe(2);
    expect(lineDisplayAmount(cart.items[0])).toEqual({
      amount: 21,
      currency: "COP",
    });
    expect(cartLinesDisplayAmount(cart)).toEqual({
      amount: 21,
      currency: "COP",
    });
  });
});
