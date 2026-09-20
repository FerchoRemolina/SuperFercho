import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, request, setUnauthenticatedHandler } from "@/shared/api/client";
import {
  clearSession,
  configureSessionPersistence,
  getSession,
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
  setUnauthenticatedHandler(null);
});

describe("request", () => {
  it("sends Bearer when a session exists and never adds userId", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });

    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await request("/cart");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/cart");
    const headers = new Headers(init.headers);
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(init.body).toBeUndefined();
  });

  it("parses problem+json and clears the session only for UNAUTHENTICATED", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const onUnauthenticated = vi.fn();
    setUnauthenticatedHandler(onUnauthenticated);

    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            title: "Unauthorized",
            status: 401,
            code: "UNAUTHENTICATED",
          }),
          {
            status: 401,
            headers: { "Content-Type": "application/problem+json" },
          },
        ),
      ),
    );

    await expect(request("/cart")).rejects.toBeInstanceOf(ApiError);
    expect(getSession()).toBeNull();
    expect(onUnauthenticated).toHaveBeenCalledTimes(1);
  });

  it("keeps the session on INVALID_CREDENTIALS", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const onUnauthenticated = vi.fn();
    setUnauthenticatedHandler(onUnauthenticated);

    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: 401,
            code: "INVALID_CREDENTIALS",
          }),
          { status: 401, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );

    await expect(request("/auth/login", { method: "POST", body: {} })).rejects.toBeInstanceOf(
      ApiError,
    );
    expect(getSession()?.accessToken).toBe("access-token");
    expect(onUnauthenticated).not.toHaveBeenCalled();
  });

  it("sends Idempotency-Key only when provided", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ id: "order" }), {
        status: 201,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await request("/orders", {
      method: "POST",
      body: {},
      idempotencyKey: "11111111-1111-1111-1111-111111111111",
    });

    const headers = new Headers(
      (fetchMock.mock.calls[0] as [string, RequestInit])[1].headers,
    );
    expect(headers.get("Idempotency-Key")).toBe(
      "11111111-1111-1111-1111-111111111111",
    );
  });
});
