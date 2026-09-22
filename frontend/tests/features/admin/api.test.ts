import { afterEach, describe, expect, it, vi } from "vitest";
import {
  activateAdminCategory,
  activateAdminProduct,
  adminKeys,
  changeAdminProductPrice,
  createAdminCategory,
  createAdminProduct,
  deactivateAdminCategory,
  deactivateAdminProduct,
  getAdminCategory,
  getAdminOrder,
  getAdminProduct,
  listAdminCategories,
  listAdminOrders,
  listAdminProducts,
  searchAdminProducts,
  updateAdminCategory,
  updateAdminProduct,
} from "@/features/admin/api";
import type { Order, PagedOrders } from "@/features/orders/api";
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

const category = {
  id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  name: "Lácteos",
  description: "Leche y derivados",
  status: "ACTIVE" as const,
  createdAt: "2026-03-01T10:00:00Z",
  updatedAt: "2026-03-01T10:30:00Z",
};

const product = {
  id: "33333333-3333-3333-3333-333333333333",
  categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  barcode: null,
  name: "Leche",
  brand: null,
  description: null,
  price: { amount: 10.5, currency: "COP" as const },
  stock: 8,
  imageUrl: null,
  status: "ACTIVE" as const,
  createdAt: "2026-03-01T10:00:00Z",
  updatedAt: "2026-03-01T10:30:00Z",
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

describe("admin catalog api", () => {
  it("sends JWT and view=ADMIN on list and search", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(jsonResponse([]));
    vi.stubGlobal("fetch", fetchMock);

    await listAdminCategories();
    await listAdminProducts({ status: "ACTIVE", categoryId: product.categoryId });
    await searchAdminProducts({ text: "leche" });

    const auth = "Bearer admin-token";
    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/categories?view=ADMIN",
    );
    expect(
      new Headers((fetchMock.mock.calls[0]?.[1] as RequestInit).headers).get(
        "Authorization",
      ),
    ).toBe(auth);
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      "http://localhost:8080/api/v1/products?view=ADMIN&categoryId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa&status=ACTIVE",
    );
    expect(fetchMock.mock.calls[2]?.[0]).toBe(
      "http://localhost:8080/api/v1/products/search?view=ADMIN&text=leche",
    );
  });

  it("creates product with POST body including stock", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(product, 201));
    vi.stubGlobal("fetch", fetchMock);

    await createAdminProduct({
      categoryId: product.categoryId,
      barcode: null,
      name: "Leche",
      brand: null,
      description: null,
      price: product.price,
      stock: 8,
      imageUrl: null,
    });

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(init.body).toBe(
      JSON.stringify({
        categoryId: product.categoryId,
        barcode: null,
        name: "Leche",
        brand: null,
        description: null,
        price: product.price,
        stock: 8,
        imageUrl: null,
      }),
    );
  });

  it("updates product with PUT without stock or price", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(product));
    vi.stubGlobal("fetch", fetchMock);

    await updateAdminProduct(product.id, {
      categoryId: product.categoryId,
      barcode: null,
      name: "Leche entera",
      brand: null,
      description: null,
      imageUrl: null,
    });

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(init.body)).not.toContain("stock");
    expect(String(init.body)).not.toContain("price");
    expect(String(init.body)).not.toContain("status");
  });

  it("changes price with POST /price only", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(product));
    vi.stubGlobal("fetch", fetchMock);

    await changeAdminProductPrice(product.id, {
      price: { amount: 12, currency: "COP" },
    });

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/products/${product.id}/price`,
    );
    expect((fetchMock.mock.calls[0]?.[1] as RequestInit).method).toBe("POST");
    expect((fetchMock.mock.calls[0]?.[1] as RequestInit).body).toBe(
      JSON.stringify({ price: { amount: 12, currency: "COP" } }),
    );
  });

  it("activates and deactivates with dedicated POST endpoints", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(product))
      .mockResolvedValueOnce(jsonResponse({ ...product, status: "INACTIVE" }));
    vi.stubGlobal("fetch", fetchMock);

    await activateAdminProduct(product.id);
    await deactivateAdminProduct(product.id);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/products/${product.id}/activate`,
    );
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      `http://localhost:8080/api/v1/products/${product.id}/deactivate`,
    );
  });

  it("loads a single product with view=ADMIN", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(product));
    vi.stubGlobal("fetch", fetchMock);

    await getAdminProduct(product.id);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/products/${product.id}?view=ADMIN`,
    );
  });

  it("uses dedicated admin query keys", () => {
    expect(adminKeys().products({ categoryId: "x", status: "ACTIVE" })).toEqual([
      "admin",
      "products",
      "list",
      "x",
      "ACTIVE",
    ]);
    expect(adminKeys().search("leche")).toEqual([
      "admin",
      "products",
      "search",
      "leche",
    ]);
    expect(adminKeys().categories()).toEqual(["admin", "categories", "list"]);
    expect(adminKeys().category(category.id)).toEqual([
      "admin",
      "category",
      category.id,
    ]);
  });

  it("creates category with POST body without status", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(category, 201));
    vi.stubGlobal("fetch", fetchMock);

    await createAdminCategory({
      name: "Lácteos",
      description: "Leche y derivados",
    });

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/categories",
    );
    expect(init.method).toBe("POST");
    expect(init.body).toBe(
      JSON.stringify({
        name: "Lácteos",
        description: "Leche y derivados",
      }),
    );
    expect(String(init.body)).not.toContain("status");
  });

  it("updates category with PUT without status", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(category));
    vi.stubGlobal("fetch", fetchMock);

    await updateAdminCategory(category.id, {
      name: "Lácteos frescos",
      description: null,
    });

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/categories/${category.id}`,
    );
    expect(init.method).toBe("PUT");
    expect(String(init.body)).not.toContain("status");
  });

  it("activates and deactivates categories with dedicated POST endpoints", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(category))
      .mockResolvedValueOnce(jsonResponse({ ...category, status: "INACTIVE" }));
    vi.stubGlobal("fetch", fetchMock);

    await activateAdminCategory(category.id);
    await deactivateAdminCategory(category.id);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/categories/${category.id}/activate`,
    );
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      `http://localhost:8080/api/v1/categories/${category.id}/deactivate`,
    );
  });

  it("loads a single category with view=ADMIN", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(category));
    vi.stubGlobal("fetch", fetchMock);

    await getAdminCategory(category.id);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/categories/${category.id}?view=ADMIN`,
    );
  });
});

const listedAdminOrder: Order = {
  id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  orderNumber: "ORD-P-1001",
  customerId: "11111111-1111-1111-1111-111111111111",
  status: "PENDING",
  items: [
    {
      id: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      productId: "33333333-3333-3333-3333-333333333333",
      productName: "Leche",
      unitPrice: { amount: 4500, currency: "COP" },
      quantity: 2,
      subtotal: { amount: 9000, currency: "COP" },
    },
  ],
  subtotal: { amount: 9000, currency: "COP" },
  total: { amount: 9000, currency: "COP" },
  shippingAddress: {
    recipientName: "Ana",
    addressLine: "Calle 1",
    additionalInfo: null,
    city: "Bogotá",
    department: "Cundinamarca",
    phone: "3001234567",
  },
  paymentId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
  createdAt: "2026-03-01T10:00:00Z",
  confirmedAt: null,
  cancelledAt: null,
  updatedAt: "2026-03-01T10:00:00Z",
  payment: null,
};

const pagedAdminOrders: PagedOrders = {
  items: [listedAdminOrder],
  page: 0,
  size: 20,
  totalElements: 1,
};

describe("admin orders api", () => {
  it("lists admin orders with JWT, page, size and without customer ownership params", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "99999999-9999-9999-9999-999999999999",
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(pagedAdminOrders));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listAdminOrders({ page: 0, size: 20 }),
    ).resolves.toEqual(pagedAdminOrders);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      "http://localhost:8080/api/v1/admin/orders?page=0&size=20",
    );
    expect(init.method ?? "GET").toBe("GET");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer admin-token",
    );
    expect(url).not.toContain("customerId");
    expect(pagedAdminOrders.items[0]?.payment).toBeNull();
  });

  it("sends status filter with page and size", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ items: [], page: 1, size: 20, totalElements: 0 }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listAdminOrders({ page: 1, size: 20, status: "CONFIRMED" }),
    ).resolves.toEqual({
      items: [],
      page: 1,
      size: 20,
      totalElements: 0,
    });

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/admin/orders?page=1&size=20&status=CONFIRMED",
    );
  });

  it("omits status when listing all admin orders", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(pagedAdminOrders));
    vi.stubGlobal("fetch", fetchMock);

    await listAdminOrders({ page: 0, size: 20 });

    expect(String(fetchMock.mock.calls[0]?.[0])).not.toContain("status=");
  });

  it("uses admin orders query keys with page size and status", () => {
    expect(adminKeys().ordersRoot()).toEqual(["admin", "orders"]);
    expect(
      adminKeys().orders({ page: 0, size: 20, status: "READY" }),
    ).toEqual(["admin", "orders", "list", 0, 20, "READY"]);
    expect(adminKeys().orders({ page: 2, size: 20 })).toEqual([
      "admin",
      "orders",
      "list",
      2,
      20,
      "all",
    ]);
  });

  it("propagates RFC7807 errors from GET /admin/orders", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          status: 400,
          code: "INVALID_ORDER",
          title: "Bad Request",
          detail: "invalid order status: SOLD",
        },
        400,
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listAdminOrders({ page: 0, size: 20, status: "PENDING" }),
    ).rejects.toMatchObject({
      problem: { status: 400, code: "INVALID_ORDER" },
    });
  });

  it("gets admin order detail by id with JWT and nested payment", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "99999999-9999-9999-9999-999999999999",
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const detailOrder: Order = {
      ...listedAdminOrder,
      payment: {
        paymentId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
        amount: { amount: 9000, currency: "COP" },
        paymentMethod: "SIMULATED_CARD",
        status: "APPROVED",
        providerReference: "sim-1",
        refundedAt: null,
        createdAt: "2026-03-01T10:00:00Z",
        updatedAt: "2026-03-01T10:00:00Z",
      },
    };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(detailOrder));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getAdminOrder(detailOrder.id)).resolves.toEqual(detailOrder);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      `http://localhost:8080/api/v1/admin/orders/${detailOrder.id}`,
    );
    expect(url).not.toContain("/api/v1/orders/");
    expect(init.method ?? "GET").toBe("GET");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer admin-token",
    );
  });

  it("parses admin order detail when payment is null", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(listedAdminOrder));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getAdminOrder(listedAdminOrder.id)).resolves.toEqual(
      listedAdminOrder,
    );
    expect(listedAdminOrder.payment).toBeNull();
  });

  it("uses admin order detail query key", () => {
    expect(
      adminKeys().order("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ).toEqual(["admin", "order", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"]);
  });

  it("propagates ORDER_NOT_FOUND from GET /admin/orders/{id}", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          status: 404,
          code: "ORDER_NOT_FOUND",
          title: "Not Found",
          detail: "Order not found",
        },
        404,
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(getAdminOrder(listedAdminOrder.id)).rejects.toMatchObject({
      problem: { status: 404, code: "ORDER_NOT_FOUND" },
    });
  });

  it("propagates PAYMENT_NOT_FOUND without treating it as payment null", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          status: 404,
          code: "PAYMENT_NOT_FOUND",
          title: "Not Found",
          detail: "Payment not found",
        },
        404,
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(getAdminOrder(listedAdminOrder.id)).rejects.toMatchObject({
      problem: { status: 404, code: "PAYMENT_NOT_FOUND" },
    });
  });
});
