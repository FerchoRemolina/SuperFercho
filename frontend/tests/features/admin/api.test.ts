import { afterEach, describe, expect, it, vi } from "vitest";
import {
  activateAdminCategory,
  activateAdminProduct,
  activateAdminProductType,
  activateAdminProductVariant,
  adjustAdminProductStock,
  adminKeys,
  ADMIN_SALES_ORDER_STATUSES,
  changeAdminProductPrice,
  createAdminCategory,
  createAdminKnowledgeDocument,
  createAdminProduct,
  createAdminProductType,
  createAdminProductVariant,
  deactivateAdminCategory,
  deactivateAdminKnowledgeDocument,
  deactivateAdminProduct,
  deactivateAdminProductType,
  deactivateAdminProductVariant,
  getAdminCategory,
  getAdminKnowledgeDocument,
  getAdminOrder,
  getAdminPayment,
  getAdminProduct,
  getAdminProductType,
  getAdminProductVariant,
  listAdminCategories,
  listAdminKnowledgeDocuments,
  listAdminOrders,
  listAdminProducts,
  listAdminProductTypes,
  listAdminProductVariants,
  processAdminKnowledgeDocument,
  reactivateAdminKnowledgeDocument,
  replaceAdminKnowledgeDocumentContent,
  searchAdminKnowledge,
  searchAdminProducts,
  updateAdminCategory,
  updateAdminOrderStatus,
  updateAdminProduct,
  updateAdminProductType,
  updateAdminProductVariant,
  type AdminPayment,
  type KnowledgeDocument,
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
  productTypeId: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  productVariantId: null as string | null,
  presentation: { quantity: 1, unit: "UNIT" as const },
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

const productType = {
  id: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  name: "Leche",
  description: "Lácteos líquidos",
  status: "ACTIVE" as const,
  createdAt: "2026-03-01T10:00:00Z",
  updatedAt: "2026-03-01T10:30:00Z",
};

const productVariant = {
  id: "cccccccc-cccc-cccc-cccc-cccccccccccc",
  productTypeId: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  name: "Entera",
  description: null,
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

  it("creates product with POST body including type, presentation, and stock", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(product, 201));
    vi.stubGlobal("fetch", fetchMock);

    await createAdminProduct({
      productTypeId: product.productTypeId,
      productVariantId: null,
      presentation: { quantity: 1, unit: "L" },
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
        productTypeId: product.productTypeId,
        productVariantId: null,
        presentation: { quantity: 1, unit: "L" },
        barcode: null,
        name: "Leche",
        brand: null,
        description: null,
        price: product.price,
        stock: 8,
        imageUrl: null,
      }),
    );
    expect(init.body).not.toContain("categoryId");
  });

  it("updates product with PUT without stock, price, or categoryId", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(product));
    vi.stubGlobal("fetch", fetchMock);

    await updateAdminProduct(product.id, {
      productTypeId: product.productTypeId,
      productVariantId: productVariant.id,
      presentation: { quantity: 900, unit: "ML" },
      barcode: null,
      name: "Leche entera",
      brand: null,
      description: null,
      imageUrl: null,
    });

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("PUT");
    const body = JSON.parse(String(init.body)) as Record<string, unknown>;
    expect(body).toEqual({
      productTypeId: product.productTypeId,
      productVariantId: productVariant.id,
      presentation: { quantity: 900, unit: "ML" },
      barcode: null,
      name: "Leche entera",
      brand: null,
      description: null,
      imageUrl: null,
    });
    expect(body).not.toHaveProperty("categoryId");
    expect(body).not.toHaveProperty("stock");
    expect(body).not.toHaveProperty("price");
  });

  it("lists product types by categoryId and variants by productTypeId", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse([productType]))
      .mockResolvedValueOnce(jsonResponse([productVariant]))
      .mockResolvedValueOnce(jsonResponse(productType))
      .mockResolvedValueOnce(jsonResponse(productVariant));
    vi.stubGlobal("fetch", fetchMock);
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      accessToken: "admin-token",
      userId: "u1",
      role: "ADMIN",
      expiresAt: "2099-01-01T00:00:00Z",
    });

    await listAdminProductTypes({ categoryId: category.id });
    await listAdminProductVariants({ productTypeId: productType.id });
    await getAdminProductType(productType.id);
    await getAdminProductVariant(productVariant.id);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/product-types?categoryId=${category.id}`,
    );
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      `http://localhost:8080/api/v1/product-variants?productTypeId=${productType.id}`,
    );
    expect(fetchMock.mock.calls[2]?.[0]).toBe(
      `http://localhost:8080/api/v1/product-types/${productType.id}`,
    );
    expect(fetchMock.mock.calls[3]?.[0]).toBe(
      `http://localhost:8080/api/v1/product-variants/${productVariant.id}`,
    );
  });

  it("creates and mutates product types and variants", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(productType, 201))
      .mockResolvedValueOnce(jsonResponse(productType))
      .mockResolvedValueOnce(jsonResponse({ ...productType, status: "INACTIVE" }))
      .mockResolvedValueOnce(jsonResponse({ ...productType, status: "ACTIVE" }))
      .mockResolvedValueOnce(jsonResponse(productVariant, 201))
      .mockResolvedValueOnce(jsonResponse(productVariant))
      .mockResolvedValueOnce(jsonResponse({ ...productVariant, status: "INACTIVE" }))
      .mockResolvedValueOnce(jsonResponse({ ...productVariant, status: "ACTIVE" }));
    vi.stubGlobal("fetch", fetchMock);
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      accessToken: "admin-token",
      userId: "u1",
      role: "ADMIN",
      expiresAt: "2099-01-01T00:00:00Z",
    });

    await createAdminProductType({
      categoryId: category.id,
      name: "Leche",
      description: "Lácteos líquidos",
    });
    await updateAdminProductType(productType.id, {
      name: "Leche",
      description: null,
    });
    await deactivateAdminProductType(productType.id);
    await activateAdminProductType(productType.id);
    await createAdminProductVariant({
      productTypeId: productType.id,
      name: "Entera",
      description: null,
    });
    await updateAdminProductVariant(productVariant.id, {
      name: "Entera",
      description: null,
    });
    await deactivateAdminProductVariant(productVariant.id);
    await activateAdminProductVariant(productVariant.id);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/product-types",
    );
    expect(fetchMock.mock.calls[4]?.[0]).toBe(
      "http://localhost:8080/api/v1/product-variants",
    );
    expect(adminKeys().productTypes(category.id)).toEqual([
      "admin",
      "product-types",
      "list",
      category.id,
    ]);
    expect(adminKeys().productVariants(productType.id)).toEqual([
      "admin",
      "product-variants",
      "list",
      productType.id,
    ]);
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

  it("adjusts stock with POST /stock only", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ ...product, stock: 25 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      adjustAdminProductStock(product.id, { stock: 25 }),
    ).resolves.toMatchObject({ stock: 25 });

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/products/${product.id}/stock`,
    );
    expect((fetchMock.mock.calls[0]?.[1] as RequestInit).method).toBe("POST");
    expect((fetchMock.mock.calls[0]?.[1] as RequestInit).body).toBe(
      JSON.stringify({ stock: 25 }),
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

  it("sends Ventas multi-status as a single comma-separated status param", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ items: [], page: 0, size: 20, totalElements: 0 }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await listAdminOrders({
      page: 0,
      size: 20,
      status: ADMIN_SALES_ORDER_STATUSES,
    });

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/admin/orders?page=0&size=20&status=CONFIRMED%2CPREPARING%2CREADY%2CDELIVERED",
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
    expect(
      adminKeys().orders({
        page: 0,
        size: 20,
        status: ADMIN_SALES_ORDER_STATUSES,
      }),
    ).toEqual(["admin", "orders", "list", 0, 20, "sales"]);
    expect(
      adminKeys().orders({ page: 0, size: 20, status: "CONFIRMED" }),
    ).not.toEqual(
      adminKeys().orders({
        page: 0,
        size: 20,
        status: ADMIN_SALES_ORDER_STATUSES,
      }),
    );
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
    expect(adminKeys().ordersRoot()).toEqual(["admin", "orders"]);
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

  it("posts admin order status update to /orders/{id}/status", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "99999999-9999-9999-9999-999999999999",
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const updated: Order = {
      ...listedAdminOrder,
      status: "CONFIRMED",
      confirmedAt: "2026-03-01T10:20:00Z",
      payment: null,
    };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(updated));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updateAdminOrderStatus(listedAdminOrder.id, { status: "CONFIRMED" }),
    ).resolves.toEqual(updated);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      `http://localhost:8080/api/v1/orders/${listedAdminOrder.id}/status`,
    );
    expect(url).not.toContain("/admin/orders/");
    expect(init.method).toBe("POST");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer admin-token",
    );
    expect(init.body).toBe(JSON.stringify({ status: "CONFIRMED" }));
    expect(updated.payment).toBeNull();
  });

  it("encodes order id in status update path", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(listedAdminOrder));
    vi.stubGlobal("fetch", fetchMock);

    await updateAdminOrderStatus("id with spaces", { status: "PREPARING" });

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/orders/id%20with%20spaces/status",
    );
  });

  it("propagates status update RFC7807 errors", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse(
          {
            status: 404,
            code: "ORDER_NOT_FOUND",
            title: "Not Found",
            detail: "missing",
          },
          404,
        ),
      )
      .mockResolvedValueOnce(
        jsonResponse(
          {
            status: 409,
            code: "INVALID_ORDER_TRANSITION",
            title: "Conflict",
            detail: "Invalid order state transition: PENDING -> PREPARING",
          },
          409,
        ),
      )
      .mockResolvedValueOnce(
        jsonResponse(
          {
            status: 400,
            code: "INVALID_ORDER_STATUS_UPDATE",
            title: "Bad Request",
            detail: "Order status cannot be updated to: CANCELLED",
          },
          400,
        ),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updateAdminOrderStatus(listedAdminOrder.id, { status: "CONFIRMED" }),
    ).rejects.toMatchObject({
      problem: { status: 404, code: "ORDER_NOT_FOUND" },
    });
    await expect(
      updateAdminOrderStatus(listedAdminOrder.id, { status: "PREPARING" }),
    ).rejects.toMatchObject({
      problem: { status: 409, code: "INVALID_ORDER_TRANSITION" },
    });
    await expect(
      updateAdminOrderStatus(listedAdminOrder.id, { status: "CANCELLED" }),
    ).rejects.toMatchObject({
      problem: { status: 400, code: "INVALID_ORDER_STATUS_UPDATE" },
    });
  });
});

const knowledgeDocument: KnowledgeDocument = {
  id: "dddddddd-dddd-dddd-dddd-dddddddddddd",
  title: "Horarios",
  source: "manual-interno",
  content: "Abrimos de 8 a 20.",
  status: "RECEIVED",
  chunks: [],
  createdAt: "2026-03-01T10:00:00Z",
  updatedAt: "2026-03-01T10:00:00Z",
};

describe("admin knowledge api", () => {
  it("lists knowledge documents with JWT", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "99999999-9999-9999-9999-999999999999",
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse([knowledgeDocument]));
    vi.stubGlobal("fetch", fetchMock);

    await expect(listAdminKnowledgeDocuments()).resolves.toEqual([
      knowledgeDocument,
    ]);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/knowledge/documents");
    expect(init.method ?? "GET").toBe("GET");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer admin-token",
    );
  });

  it("gets a knowledge document by encoded id", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(knowledgeDocument))
      .mockResolvedValueOnce(jsonResponse(knowledgeDocument));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      getAdminKnowledgeDocument(knowledgeDocument.id),
    ).resolves.toEqual(knowledgeDocument);
    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/knowledge/documents/${knowledgeDocument.id}`,
    );

    await getAdminKnowledgeDocument("id with spaces");
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      "http://localhost:8080/api/v1/knowledge/documents/id%20with%20spaces",
    );
  });

  it("creates a knowledge document with title source and content", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "99999999-9999-9999-9999-999999999999",
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse(knowledgeDocument, 201));
    vi.stubGlobal("fetch", fetchMock);

    const body = {
      title: "Horarios",
      source: "manual-interno",
      content: "Abrimos de 8 a 20.",
    };
    await expect(createAdminKnowledgeDocument(body)).resolves.toEqual(
      knowledgeDocument,
    );

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/knowledge/documents");
    expect(init.method).toBe("POST");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer admin-token",
    );
    expect(init.body).toBe(JSON.stringify(body));
  });

  it("replaces knowledge document content with PUT body", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ ...knowledgeDocument, content: "Nuevo", status: "RECEIVED" }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await replaceAdminKnowledgeDocumentContent(knowledgeDocument.id, {
      content: "Nuevo",
    });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      `http://localhost:8080/api/v1/knowledge/documents/${knowledgeDocument.id}/content`,
    );
    expect(init.method).toBe("PUT");
    expect(init.body).toBe(JSON.stringify({ content: "Nuevo" }));
  });

  it("posts process deactivate and reactivate endpoints", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ ...knowledgeDocument, status: "READY" }))
      .mockResolvedValueOnce(
        jsonResponse({ ...knowledgeDocument, status: "INACTIVE" }),
      )
      .mockResolvedValueOnce(jsonResponse({ ...knowledgeDocument, status: "READY" }));
    vi.stubGlobal("fetch", fetchMock);

    await processAdminKnowledgeDocument(knowledgeDocument.id);
    await deactivateAdminKnowledgeDocument(knowledgeDocument.id);
    await reactivateAdminKnowledgeDocument(knowledgeDocument.id);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `http://localhost:8080/api/v1/knowledge/documents/${knowledgeDocument.id}/process`,
    );
    expect((fetchMock.mock.calls[0]?.[1] as RequestInit).method).toBe("POST");
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      `http://localhost:8080/api/v1/knowledge/documents/${knowledgeDocument.id}/deactivate`,
    );
    expect(fetchMock.mock.calls[2]?.[0]).toBe(
      `http://localhost:8080/api/v1/knowledge/documents/${knowledgeDocument.id}/reactivate`,
    );
  });

  it("uses knowledge query keys", () => {
    expect(adminKeys().knowledgeRoot()).toEqual(["admin", "knowledge"]);
    expect(adminKeys().knowledgeDocuments()).toEqual([
      "admin",
      "knowledge",
      "documents",
    ]);
    expect(adminKeys().knowledgeDocument(knowledgeDocument.id)).toEqual([
      "admin",
      "knowledge",
      "document",
      knowledgeDocument.id,
    ]);
  });

  it("propagates knowledge RFC7807 errors", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse(
          {
            status: 404,
            code: "DOCUMENT_NOT_FOUND",
            title: "Not Found",
            detail: "missing",
          },
          404,
        ),
      )
      .mockResolvedValueOnce(
        jsonResponse(
          {
            status: 400,
            code: "INVALID_DOCUMENT",
            title: "Bad Request",
            detail: "document can only be processed when RECEIVED, CHUNKED or FAILED",
          },
          400,
        ),
      )
      .mockResolvedValueOnce(
        jsonResponse(
          {
            status: 409,
            code: "KNOWLEDGE_PROCESSING_FAILED",
            title: "Conflict",
            detail: "document processing failed",
          },
          409,
        ),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      getAdminKnowledgeDocument(knowledgeDocument.id),
    ).rejects.toMatchObject({
      problem: { status: 404, code: "DOCUMENT_NOT_FOUND" },
    });
    await expect(
      processAdminKnowledgeDocument(knowledgeDocument.id),
    ).rejects.toMatchObject({
      problem: { status: 400, code: "INVALID_DOCUMENT" },
    });
    await expect(
      processAdminKnowledgeDocument(knowledgeDocument.id),
    ).rejects.toMatchObject({
      problem: { status: 409, code: "KNOWLEDGE_PROCESSING_FAILED" },
    });
  });
});

const adminPayment: AdminPayment = {
  id: "55555555-5555-5555-5555-555555555555",
  orderId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  amount: { amount: 42.5, currency: "COP" },
  paymentMethod: "SIMULATED_CARD",
  status: "APPROVED",
  providerReference: "sim-approved",
  createdAt: "2026-03-01T10:00:00Z",
  updatedAt: "2026-03-01T10:00:00Z",
  refundedAt: null,
};

describe("admin payment api", () => {
  it("gets a payment by encoded id with JWT", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "99999999-9999-9999-9999-999999999999",
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(adminPayment))
      .mockResolvedValueOnce(jsonResponse(adminPayment));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getAdminPayment(adminPayment.id)).resolves.toEqual(adminPayment);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      `http://localhost:8080/api/v1/payments/${adminPayment.id}`,
    );
    expect(init.method ?? "GET").toBe("GET");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer admin-token",
    );

    await getAdminPayment("id with spaces");
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      "http://localhost:8080/api/v1/payments/id%20with%20spaces",
    );
  });

  it("maps payment query keys", () => {
    expect(adminKeys().paymentsRoot()).toEqual(["admin", "payments"]);
    expect(adminKeys().payment(adminPayment.id)).toEqual([
      "admin",
      "payment",
      adminPayment.id,
    ]);
  });

  it("propagates PAYMENT_NOT_FOUND", async () => {
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

    await expect(getAdminPayment(adminPayment.id)).rejects.toMatchObject({
      problem: { status: 404, code: "PAYMENT_NOT_FOUND" },
    });
  });
});

describe("admin knowledge search api", () => {
  it("searches knowledge with query limit and JWT", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "99999999-9999-9999-9999-999999999999",
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const result = {
      hits: [
        {
          documentId: "dddddddd-dddd-dddd-dddd-dddddddddddd",
          chunkId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
          title: "Horarios",
          source: "manual-interno",
          chunkText: "Abrimos de 8 a 20.",
          score: 0.91,
        },
      ],
    };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(result));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      searchAdminKnowledge({ query: "horario", limit: 10 }),
    ).resolves.toEqual(result);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      "http://localhost:8080/api/v1/knowledge/search?query=horario&limit=10",
    );
    expect(init.method ?? "GET").toBe("GET");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer admin-token",
    );
  });

  it("maps knowledge search query keys", () => {
    expect(adminKeys().knowledgeSearch("horario", 10)).toEqual([
      "admin",
      "knowledge",
      "search",
      "horario",
      10,
    ]);
  });

  it("propagates INVALID_SEARCH_REQUEST and KNOWLEDGE_PROCESSING_FAILED", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse(
          {
            status: 400,
            code: "INVALID_SEARCH_REQUEST",
            title: "Bad Request",
            detail: "limit must be between 1 and 20",
          },
          400,
        ),
      )
      .mockResolvedValueOnce(
        jsonResponse(
          {
            status: 409,
            code: "KNOWLEDGE_PROCESSING_FAILED",
            title: "Conflict",
            detail: "failed to search knowledge",
          },
          409,
        ),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      searchAdminKnowledge({ query: "x", limit: 99 }),
    ).rejects.toMatchObject({
      problem: { status: 400, code: "INVALID_SEARCH_REQUEST" },
    });
    await expect(
      searchAdminKnowledge({ query: "x", limit: 5 }),
    ).rejects.toMatchObject({
      problem: { status: 409, code: "KNOWLEDGE_PROCESSING_FAILED" },
    });
  });
});
