import { afterEach, describe, expect, it, vi } from "vitest";
import { lookupProductByBarcode } from "@/features/admin/api";
import {
  applyBarcodeSuggestion,
  emptyAdminProductFormValues,
} from "@/features/admin/payloads";
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

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  clearSession();
  configureSessionPersistence(null);
});

describe("admin barcode lookup api", () => {
  it("calls GET /products/barcode-lookup/{barcode} with JWT", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "ADMIN",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          barcode: "3017620422003",
          name: "Nutella",
          brand: "Ferrero",
          description: "Spread",
          imageUrl: "https://img.test/n.png",
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await lookupProductByBarcode("3017620422003");

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      "http://localhost:8080/api/v1/products/barcode-lookup/3017620422003",
    );
    expect(init.method ?? "GET").toBe("GET");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer access-token",
    );
    expect(result.name).toBe("Nutella");
  });

  it("surfaces BARCODE_LOOKUP_NOT_FOUND", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "ADMIN",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: 404,
            code: "BARCODE_LOOKUP_NOT_FOUND",
            detail: "No product found",
          }),
          {
            status: 404,
            headers: { "Content-Type": "application/problem+json" },
          },
        ),
      ),
    );

    await expect(lookupProductByBarcode("00000000")).rejects.toMatchObject({
      problem: { code: "BARCODE_LOOKUP_NOT_FOUND" },
    });
  });
});

describe("applyBarcodeSuggestion", () => {
  it("fills suggestion fields without changing price, stock, categoryId or taxonomy", () => {
    const current = {
      ...emptyAdminProductFormValues(),
      categoryId: "cat-1",
      productTypeId: "type-1",
      productVariantId: "var-1",
      presentationQuantity: "2",
      presentationUnit: "L" as const,
      price: "4500",
      stock: "12",
      name: "Viejo",
      brand: "Vieja",
    };

    const next = applyBarcodeSuggestion(current, {
      barcode: "3017620422003",
      name: "Nutella",
      brand: "Ferrero",
      description: "Crema",
      imageUrl: "https://img.test/n.png",
    });

    expect(next.barcode).toBe("3017620422003");
    expect(next.name).toBe("Nutella");
    expect(next.brand).toBe("Ferrero");
    expect(next.description).toBe("Crema");
    expect(next.imageUrl).toBe("https://img.test/n.png");
    expect(next.categoryId).toBe("cat-1");
    expect(next.productTypeId).toBe("type-1");
    expect(next.productVariantId).toBe("var-1");
    expect(next.presentationQuantity).toBe("2");
    expect(next.presentationUnit).toBe("L");
    expect(next.price).toBe("4500");
    expect(next.stock).toBe("12");
  });

  it("keeps existing values when suggestion fields are null", () => {
    const current = {
      ...emptyAdminProductFormValues(),
      name: "Manual",
      brand: "Local",
      description: "Desc",
      imageUrl: "https://img.test/local.png",
      price: "1000",
      stock: "3",
      categoryId: "cat-2",
    };

    const next = applyBarcodeSuggestion(current, {
      barcode: "12345678",
      name: null,
      brand: null,
      description: null,
      imageUrl: null,
    });

    expect(next.barcode).toBe("12345678");
    expect(next.name).toBe("Manual");
    expect(next.brand).toBe("Local");
    expect(next.description).toBe("Desc");
    expect(next.imageUrl).toBe("https://img.test/local.png");
    expect(next.price).toBe("1000");
    expect(next.stock).toBe("3");
    expect(next.categoryId).toBe("cat-2");
  });
});
