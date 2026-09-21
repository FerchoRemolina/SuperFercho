import { afterEach, describe, expect, it, vi } from "vitest";
import type { CartItem } from "@/features/cart/api";
import type { Product } from "@/features/catalog/api";
import {
  cancelOrder,
  checkout,
  getOrder,
  listOrders,
  orderKeys,
  type CheckoutRequest,
  type CheckoutResponse,
  type Order,
  type PagedOrders,
} from "@/features/orders/api";
import { buildCheckoutItems } from "@/features/orders/checkout-prices";
import {
  checkoutFingerprint,
  keyForAttempt,
} from "@/features/orders/idempotency";
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

const checkoutBody: CheckoutRequest = {
  addressId: "22222222-2222-2222-2222-222222222222",
  paymentMethod: "SIMULATED_CARD",
  items: [
    {
      productId: "33333333-3333-3333-3333-333333333333",
      quantity: 2,
      expectedUnitPrice: { amount: 10.5, currency: "COP" },
    },
  ],
};

const checkoutResponse: CheckoutResponse = {
  orderId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  orderNumber: "ORD-P-1001",
  status: "PENDING",
  paymentStatus: "APPROVED",
  total: { amount: 21, currency: "COP" },
};

const order: Order = {
  id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  orderNumber: "ORD-P-1001",
  customerId: "11111111-1111-1111-1111-111111111111",
  status: "PENDING",
  items: [
    {
      id: "99999999-9999-9999-9999-000000000001",
      productId: "33333333-3333-3333-3333-333333333333",
      productName: "Leche entera",
      unitPrice: { amount: 10.5, currency: "COP" },
      quantity: 2,
      subtotal: { amount: 21, currency: "COP" },
    },
  ],
  subtotal: { amount: 21, currency: "COP" },
  total: { amount: 21, currency: "COP" },
  shippingAddress: {
    recipientName: "Ada Lovelace",
    addressLine: "Calle 1 # 2-3",
    additionalInfo: "Apto 101",
    city: "Bogotá",
    department: "Cundinamarca",
    phone: "3001234567",
  },
  paymentId: "55555555-5555-5555-5555-555555555555",
  createdAt: "2026-03-01T10:00:00Z",
  confirmedAt: null,
  cancelledAt: null,
  updatedAt: "2026-03-01T10:00:00Z",
  payment: {
    paymentId: "55555555-5555-5555-5555-555555555555",
    amount: { amount: 21, currency: "COP" },
    paymentMethod: "SIMULATED_CARD",
    status: "APPROVED",
    providerReference: "sim-approved",
    refundedAt: null,
    createdAt: "2026-03-01T10:00:00Z",
    updatedAt: "2026-03-01T10:00:00Z",
  },
};

const listedOrder: Order = {
  ...order,
  payment: null,
};

const pagedOrders: PagedOrders = {
  items: [listedOrder],
  page: 0,
  size: 20,
  totalElements: 1,
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

describe("orders api", () => {
  it("posts CheckoutRequest with Idempotency-Key and JWT, without customerId", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse(checkoutResponse, 201));
    vi.stubGlobal("fetch", fetchMock);

    await expect(checkout(checkoutBody, "checkout-key-1")).resolves.toEqual(
      checkoutResponse,
    );

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/orders");
    expect(init.method).toBe("POST");
    const headers = new Headers(init.headers);
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(headers.get("Idempotency-Key")).toBe("checkout-key-1");
    expect(init.body).toBe(JSON.stringify(checkoutBody));
    expect(String(init.body)).not.toContain("customerId");
    expect(String(init.body)).not.toContain("priceAtAddition");
  });

  it("gets an owned order by id", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(order));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      getOrder("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ).resolves.toEqual(order);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/orders/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    );
  });

  it("propagates RFC7807 404 from GET /orders/{id}", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          status: 404,
          code: "ORDER_NOT_FOUND",
          title: "Not Found",
          detail: "Order not found: aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        },
        404,
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      getOrder("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ).rejects.toMatchObject({
      problem: { status: 404, code: "ORDER_NOT_FOUND" },
    });
  });

  it("uses list and detail query keys", () => {
    expect(orderKeys().all).toEqual(["orders"]);
    expect(
      orderKeys().detail("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ).toEqual(["orders", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"]);
  });

  it("lists owned orders with JWT and without customerId", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(pagedOrders));
    vi.stubGlobal("fetch", fetchMock);

    await expect(listOrders()).resolves.toEqual(pagedOrders);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/orders");
    expect(init.method ?? "GET").toBe("GET");
    const headers = new Headers(init.headers);
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(url).not.toContain("customerId");
    expect(url).not.toContain("userId");
    expect(url).not.toContain("role");
    expect(pagedOrders.items[0]?.payment).toBeNull();
  });

  it("sends optional page and size when listing orders", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ items: [], page: 1, size: 10, totalElements: 0 }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(listOrders({ page: 1, size: 10 })).resolves.toEqual({
      items: [],
      page: 1,
      size: 10,
      totalElements: 0,
    });

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/orders?page=1&size=10",
    );
  });

  it("propagates RFC7807 errors from GET /orders", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          status: 500,
          code: "INTERNAL_ERROR",
          title: "Internal Server Error",
          detail: "boom",
        },
        500,
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(listOrders()).rejects.toMatchObject({
      problem: { status: 500, code: "INTERNAL_ERROR" },
    });
  });

  it("posts cancel without body, customerId or extra headers", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const cancelled: Order = {
      ...order,
      status: "CANCELLED",
      cancelledAt: "2026-03-01T10:10:00Z",
      payment: null,
    };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(cancelled));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      cancelOrder("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ).resolves.toEqual(cancelled);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      "http://localhost:8080/api/v1/orders/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/cancel",
    );
    expect(init.method).toBe("POST");
    expect(init.body).toBeUndefined();
    const headers = new Headers(init.headers);
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(headers.has("Idempotency-Key")).toBe(false);
    expect(headers.has("Content-Type")).toBe(false);
  });

  it("propagates CANCELLATION_NOT_ALLOWED from POST cancel", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          status: 409,
          code: "CANCELLATION_NOT_ALLOWED",
          title: "Conflict",
          detail: "customer cancellation window has expired",
        },
        409,
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      cancelOrder("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ).rejects.toMatchObject({
      problem: { status: 409, code: "CANCELLATION_NOT_ALLOWED" },
    });
  });
});

describe("expectedUnitPrice from catalog", () => {
  it("uses the current catalog price, not priceAtAddition", () => {
    const items: CartItem[] = [
      {
        id: "99999999-9999-9999-9999-000000000001",
        productId: "33333333-3333-3333-3333-333333333333",
        quantity: 2,
        priceAtAddition: { amount: 9, currency: "COP" },
        addedAt: "2026-04-01T10:00:00Z",
        updatedAt: "2026-04-01T10:05:00Z",
      },
    ];
    const product: Product = {
      id: "33333333-3333-3333-3333-333333333333",
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      barcode: null,
      name: "Leche entera",
      brand: null,
      description: null,
      price: { amount: 10.5, currency: "COP" },
      stock: 20,
      imageUrl: null,
      status: "ACTIVE",
      createdAt: "2026-03-01T10:00:00Z",
      updatedAt: "2026-03-01T10:30:00Z",
    };

    expect(buildCheckoutItems(items, new Map([[product.id, product]]))).toEqual(
      [
        {
          productId: product.id,
          quantity: 2,
          expectedUnitPrice: { amount: 10.5, currency: "COP" },
        },
      ],
    );
  });
});

describe("idempotency key reuse", () => {
  it("keeps the same key while the fingerprint is unchanged", () => {
    vi.spyOn(crypto, "randomUUID")
      .mockReturnValueOnce("11111111-1111-1111-1111-111111111111")
      .mockReturnValueOnce("22222222-2222-2222-2222-222222222222");

    const fingerprint = checkoutFingerprint(checkoutBody);
    const first = keyForAttempt(null, fingerprint);
    const second = keyForAttempt(first, fingerprint);

    expect(first.key).toBe("11111111-1111-1111-1111-111111111111");
    expect(second.key).toBe(first.key);
  });

  it("issues a new key when address, payment, quantities or prices change", () => {
    vi.spyOn(crypto, "randomUUID")
      .mockReturnValueOnce("11111111-1111-1111-1111-111111111111")
      .mockReturnValueOnce("22222222-2222-2222-2222-222222222222");

    const first = keyForAttempt(null, checkoutFingerprint(checkoutBody));
    const changed = keyForAttempt(
      first,
      checkoutFingerprint({
        ...checkoutBody,
        paymentMethod: "CASH_ON_DELIVERY",
      }),
    );

    expect(changed.key).toBe("22222222-2222-2222-2222-222222222222");
    expect(changed.key).not.toBe(first.key);
  });
});
