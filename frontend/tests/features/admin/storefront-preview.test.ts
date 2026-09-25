import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  adminSessionFromStorefrontPreview,
  exitStorefrontPreview,
  getStorefrontPreview,
  previewMetaFromStorefrontPreview,
  sessionFromStorefrontPreview,
  startStorefrontPreview,
  type StorefrontPreviewSessionRestResponse,
} from "@/features/admin/storefront-preview-api";
import {
  clearSession,
  configureSessionPersistence,
  setSession,
  type SessionPersistence,
} from "@/shared/session/session";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

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

const previewResponse: StorefrontPreviewSessionRestResponse = {
  previewId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
  adminUserId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  temporaryCustomerId: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  role: "CUSTOMER",
  status: "ACTIVE",
  previewCreatedAt: "2099-01-01T00:00:00Z",
  previewExpiresAt: "2099-01-01T00:20:00Z",
  remainingSeconds: 1200,
  accessToken: "preview-customer-token",
  accessTokenExpiresAt: "2099-01-01T00:20:00Z",
  adminAccessToken: "admin-token-fresh",
  adminAccessTokenExpiresAt: "2099-01-01T00:20:00Z",
};

afterEach(() => {
  clearSession();
  configureSessionPersistence(null);
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("storefront-preview API", () => {
  it("maps start response into Customer session, Admin stash session and preview meta", () => {
    expect(sessionFromStorefrontPreview(previewResponse)).toEqual({
      userId: previewResponse.temporaryCustomerId,
      role: "CUSTOMER",
      accessToken: previewResponse.accessToken,
      expiresAt: previewResponse.accessTokenExpiresAt,
    });
    expect(adminSessionFromStorefrontPreview(previewResponse)).toEqual({
      userId: previewResponse.adminUserId,
      role: "ADMIN",
      accessToken: previewResponse.adminAccessToken,
      expiresAt: previewResponse.adminAccessTokenExpiresAt,
    });
    expect(previewMetaFromStorefrontPreview(previewResponse)).toEqual({
      previewId: previewResponse.previewId,
      previewExpiresAt: previewResponse.previewExpiresAt,
      temporaryCustomerId: previewResponse.temporaryCustomerId,
    });
  });

  it("POSTs start preview with the Admin bearer", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: previewResponse.adminUserId,
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(previewResponse), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await startStorefrontPreview();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/api/v1/admin/storefront-preview");
    expect(init.method).toBe("POST");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer admin-token",
    );
  });

  it("GETs preview status", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: previewResponse.adminUserId,
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          previewId: previewResponse.previewId,
          adminUserId: previewResponse.adminUserId,
          temporaryCustomerId: previewResponse.temporaryCustomerId,
          status: "ACTIVE",
          previewCreatedAt: previewResponse.previewCreatedAt,
          previewExpiresAt: previewResponse.previewExpiresAt,
          remainingSeconds: 600,
          usable: true,
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    const status = await getStorefrontPreview();
    expect(status.usable).toBe(true);
    expect(fetchMock.mock.calls[0]?.[0]).toContain(
      "/api/v1/admin/storefront-preview",
    );
  });

  it("POSTs exit with an explicit Admin bearer while Customer is active", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: previewResponse.temporaryCustomerId,
      role: "CUSTOMER",
      accessToken: "customer-preview-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await exitStorefrontPreview("admin-token");

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain(
      "/api/v1/admin/storefront-preview/exit",
    );
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer admin-token",
    );
  });
});

describe("storefront-preview composition", () => {
  it("wires Ver tienda, banner, dual-session APIs and cache clears", () => {
    const provider = source("shared/session/session-provider.tsx");
    const header = source("shared/ui/site-header.tsx");
    const nav = source("features/admin/components/admin-section-nav.tsx");
    const banner = source("shared/session/storefront-preview-banner.tsx");
    const enter = source(
      "features/admin/components/storefront-preview-enter-button.tsx",
    );
    const customerCountdown = source(
      "shared/session/customer-session-countdown.tsx",
    );

    expect(enter).toContain("Ver tienda");
    expect(nav).toContain("StorefrontPreviewEnterButton");
    expect(header).toContain("StorefrontPreviewBanner");
    expect(header).toContain("CustomerSessionCountdown");
    expect(header).toContain("Volver a administración");
    expect(header).toContain("Salir de preview");
    expect(banner).toContain("Modo de prueba");
    expect(customerCountdown).toContain("session.expiresAt");
    expect(customerCountdown).toContain("isPreview");
    expect(customerCountdown).toContain("clearSession");
    expect(customerCountdown).not.toContain("logout");
    expect(customerCountdown).not.toContain("request(");
    expect(customerCountdown).not.toContain("fetch(");

    expect(provider).toContain("enterStorefrontPreview");
    expect(provider).toContain("returnToAdmin");
    expect(provider).toContain("exitStorefrontPreviewMode");
    expect(provider).toContain("removeQueries");
    expect(provider).toContain("reconcilePreviewSessionOnHydrate");
    expect(provider).toContain("logoutInFlightRef");
    expect(provider).not.toContain("resolvePreviewIdleAction");
  });

  it("keeps RequireRole unchanged and hides Admin nav during Customer preview", () => {
    const requireRole = source("shared/auth/require-role.tsx");
    const header = source("shared/ui/site-header.tsx");

    expect(requireRole).toContain("session.role !== role");
    expect(header).toContain("isAdmin ?");
    expect(header).toContain('href="/admin"');
    expect(header).toContain("isPreview");
  });

  it("does not redesign Home or Admin hub in this phase", () => {
    const hub = source("features/admin/components/admin-hub.tsx");
    expect(hub).not.toContain("Ver tienda");
    expect(hub).not.toContain("StorefrontPreview");
  });
});
