import { afterEach, describe, expect, it, vi } from "vitest";
import type { StorefrontPreviewSessionRestResponse } from "@/features/admin/storefront-preview-api";
import {
  activateStorefrontPreviewSession,
  adminTokenForPreviewExit,
  applyPreviewUnauthenticatedRestore,
  clearAllSessionState,
  closePreviewAndRestoreAdmin,
  hasActivePreviewState,
  reconcilePreviewSessionOnHydrate,
  resolvePreviewUnauthenticatedAction,
  restoreAdminFromPreviewStash,
} from "@/shared/session/preview-orchestration";
import {
  ADMIN_SESSION_STORAGE_KEY,
  PREVIEW_META_STORAGE_KEY,
  clearPreviewState,
  formatPreviewRemaining,
  getPreviewMeta,
  isStorefrontPreviewActive,
  previewRemainingMs,
  readStashedAdminSession,
  setPreviewMeta,
  stashAdminSession,
} from "@/shared/session/preview-session";
import {
  formatSessionRemaining,
  remainingMsUntil,
} from "@/shared/session/session-countdown";
import {
  clearSession,
  configureSessionPersistence,
  getSession,
  hydrateSession,
  isExpired,
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

const adminSession = {
  userId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  role: "ADMIN" as const,
  accessToken: "admin-token-old",
  expiresAt: "2099-01-01T00:10:00Z",
};

const customerSession = {
  userId: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  role: "CUSTOMER" as const,
  accessToken: "customer-token",
  expiresAt: "2099-01-01T00:00:00Z",
};

const previewResponse: StorefrontPreviewSessionRestResponse = {
  previewId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
  adminUserId: adminSession.userId,
  temporaryCustomerId: customerSession.userId,
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
  clearAllSessionState();
  configureSessionPersistence(null);
  vi.useRealTimers();
});

describe("preview-session stash and meta", () => {
  it("stashes and restores the Admin session", () => {
    configureSessionPersistence(new MemoryPersistence());
    stashAdminSession(adminSession);
    expect(readStashedAdminSession()).toEqual(adminSession);
  });

  it("does not stash a Customer session", () => {
    const store = new MemoryPersistence();
    configureSessionPersistence(store);
    stashAdminSession(customerSession);
    expect(store.getItem(ADMIN_SESSION_STORAGE_KEY)).toBeNull();
  });

  it("persists preview meta separately from the active session", () => {
    const store = new MemoryPersistence();
    configureSessionPersistence(store);
    setPreviewMeta({
      previewId: previewResponse.previewId,
      previewExpiresAt: previewResponse.previewExpiresAt,
      temporaryCustomerId: previewResponse.temporaryCustomerId,
    });
    expect(getPreviewMeta()?.previewId).toBe(previewResponse.previewId);
    expect(store.getItem(PREVIEW_META_STORAGE_KEY)).toContain(
      previewResponse.previewId,
    );
  });

  it("detects an active storefront preview only for the temporary customer", () => {
    configureSessionPersistence(new MemoryPersistence());
    const meta = {
      previewId: previewResponse.previewId,
      previewExpiresAt: previewResponse.previewExpiresAt,
      temporaryCustomerId: previewResponse.temporaryCustomerId,
    };
    expect(
      isStorefrontPreviewActive(
        { ...customerSession, userId: previewResponse.temporaryCustomerId },
        meta,
      ),
    ).toBe(true);
    expect(isStorefrontPreviewActive(adminSession, meta)).toBe(false);
    expect(isStorefrontPreviewActive(customerSession, null)).toBe(false);
  });
});

describe("session and preview countdown formatting", () => {
  it("formats remaining time from previewExpiresAt", () => {
    const meta = {
      previewId: "p",
      previewExpiresAt: "2099-01-01T00:17:32Z",
      temporaryCustomerId: "c",
    };
    const now = Date.parse("2099-01-01T00:00:00Z");
    expect(previewRemainingMs(meta, now)).toBe(17 * 60_000 + 32_000);
    expect(formatPreviewRemaining(17 * 60_000 + 32_000)).toBe(
      "17:32 restantes",
    );
  });

  it("formats Customer session remaining from expiresAt", () => {
    const now = Date.parse("2099-01-01T00:00:00Z");
    expect(remainingMsUntil("2099-01-01T00:17:32Z", now)).toBe(
      17 * 60_000 + 32_000,
    );
    expect(formatSessionRemaining(17 * 60_000 + 32_000)).toBe("17:32");
    expect(formatSessionRemaining(59_000)).toBe("Menos de 1 min");
  });

  it("treats remainingMs <= 0 as session due for local clear", () => {
    const now = Date.parse("2099-01-01T00:20:00Z");
    expect(remainingMsUntil("2099-01-01T00:20:00Z", now)).toBe(0);
    expect(remainingMsUntil("2099-01-01T00:19:59Z", now)).toBe(0);
  });

  it("clearSession removes a Customer session when countdown reaches zero", () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      ...customerSession,
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const now = Date.parse("2099-01-01T00:00:00Z");
    expect(remainingMsUntil("2099-01-01T00:00:00Z", now)).toBe(0);
    clearSession();
    expect(getSession()).toBeNull();
  });

  it("uses a near-expiry wording under two minutes for preview", () => {
    expect(formatPreviewRemaining(2 * 60_000)).toBe("Quedan 2:00");
    expect(formatPreviewRemaining(59_000)).toBe("Queda menos de 1 minuto");
    expect(formatPreviewRemaining(0)).toBe("La preview ya no es usable");
  });
});

describe("preview orchestration", () => {
  it("stashes the fresh Admin JWT from start, not the pre-preview token", () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession(adminSession);

    activateStorefrontPreviewSession(adminSession, previewResponse);

    expect(readStashedAdminSession()).toEqual({
      userId: previewResponse.adminUserId,
      role: "ADMIN",
      accessToken: "admin-token-fresh",
      expiresAt: previewResponse.adminAccessTokenExpiresAt,
    });
    expect(readStashedAdminSession()?.accessToken).not.toBe("admin-token-old");
    expect(getSession()?.accessToken).toBe("preview-customer-token");
  });

  it("reuses the same preview meta when Admin re-enters Ver tienda", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, previewResponse);
    restoreAdminFromPreviewStash();

    expect(getSession()?.accessToken).toBe("admin-token-fresh");
    expect(readStashedAdminSession()).toBeNull();
    expect(getPreviewMeta()?.previewId).toBe(previewResponse.previewId);

    const reused: StorefrontPreviewSessionRestResponse = {
      ...previewResponse,
      accessToken: "preview-customer-token-2",
      accessTokenExpiresAt: "2099-01-01T00:19:00Z",
      adminAccessToken: "admin-token-fresh-2",
      adminAccessTokenExpiresAt: "2099-01-01T00:19:00Z",
    };
    activateStorefrontPreviewSession(
      {
        userId: adminSession.userId,
        role: "ADMIN",
        accessToken: "admin-token-fresh",
        expiresAt: previewResponse.adminAccessTokenExpiresAt,
      },
      reused,
    );

    expect(getPreviewMeta()?.previewId).toBe(previewResponse.previewId);
    expect(getSession()?.accessToken).toBe("preview-customer-token-2");
    expect(readStashedAdminSession()?.accessToken).toBe("admin-token-fresh-2");
  });

  it("returns to Admin without clearing preview meta", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, previewResponse);

    const restored = restoreAdminFromPreviewStash();

    expect(restored?.accessToken).toBe("admin-token-fresh");
    expect(getSession()?.role).toBe("ADMIN");
    expect(readStashedAdminSession()).toBeNull();
    expect(getPreviewMeta()).not.toBeNull();
    expect(isStorefrontPreviewActive(getSession(), getPreviewMeta())).toBe(
      false,
    );
  });

  it("does not restore an expired Admin stash", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, {
      ...previewResponse,
      adminAccessTokenExpiresAt: "2020-01-01T00:00:00Z",
    });

    expect(restoreAdminFromPreviewStash()).toBeNull();
    expect(readStashedAdminSession()).toBeNull();
    expect(getPreviewMeta()).not.toBeNull();
  });

  it("exits preview by clearing meta/stash and restoring Admin", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, previewResponse);

    const { admin } = closePreviewAndRestoreAdmin();

    expect(admin?.accessToken).toBe("admin-token-fresh");
    expect(getSession()?.role).toBe("ADMIN");
    expect(getPreviewMeta()).toBeNull();
    expect(readStashedAdminSession()).toBeNull();
  });

  it("logout cleanup clears session, stash and preview meta", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, previewResponse);
    expect(hasActivePreviewState()).toBe(true);

    clearAllSessionState();

    expect(getSession()).toBeNull();
    expect(getPreviewMeta()).toBeNull();
    expect(readStashedAdminSession()).toBeNull();
  });

  it("resolves Admin token for exit from stash or current Admin session", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, previewResponse);
    expect(adminTokenForPreviewExit()).toBe("admin-token-fresh");

    restoreAdminFromPreviewStash();
    expect(adminTokenForPreviewExit()).toBe("admin-token-fresh");
  });

  it("restores Admin on preview 401 when stash and meta exist", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, previewResponse);
    clearSession();

    expect(resolvePreviewUnauthenticatedAction()).toBe("restore-admin");
    const restored = applyPreviewUnauthenticatedRestore();

    expect(restored?.accessToken).toBe("admin-token-fresh");
    expect(getPreviewMeta()).toBeNull();
  });

  it("does not restore Admin on 401 after return-to-admin (stash cleared)", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, previewResponse);
    restoreAdminFromPreviewStash();
    clearSession();

    expect(resolvePreviewUnauthenticatedAction()).toBe("logout");
    expect(applyPreviewUnauthenticatedRestore()).toBeNull();
  });

  it("does not restore Admin on 401 when stash JWT is expired", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, {
      ...previewResponse,
      adminAccessTokenExpiresAt: "2020-01-01T00:00:00Z",
    });
    clearSession();

    expect(resolvePreviewUnauthenticatedAction()).toBe("logout");
    expect(applyPreviewUnauthenticatedRestore()).toBeNull();
  });
});

describe("hydrate / F5 reconciliation", () => {
  it("keeps a valid Customer session without preview meta", () => {
    const store = new MemoryPersistence();
    configureSessionPersistence(store);
    setSession(customerSession);
    clearPreviewState();

    hydrateSession();
    const result = reconcilePreviewSessionOnHydrate();

    expect(result.restoredAdmin).toBe(false);
    expect(getSession()).toEqual(customerSession);
  });

  it("clears an expired Customer session on hydrate", () => {
    const store = new MemoryPersistence();
    configureSessionPersistence(store);
    setSession({ ...customerSession, expiresAt: "2020-01-01T00:00:00Z" });

    expect(hydrateSession()).toBeNull();
    expect(getSession()).toBeNull();
  });

  it("keeps Preview when Customer JWT and meta still match", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, previewResponse);

    hydrateSession();
    reconcilePreviewSessionOnHydrate();

    expect(isStorefrontPreviewActive(getSession(), getPreviewMeta())).toBe(
      true,
    );
    expect(readStashedAdminSession()?.accessToken).toBe("admin-token-fresh");
  });

  it("restores Admin when Customer session is gone but stash+meta remain", () => {
    configureSessionPersistence(new MemoryPersistence());
    activateStorefrontPreviewSession(adminSession, previewResponse);
    clearSession();

    const result = reconcilePreviewSessionOnHydrate();

    expect(result.restoredAdmin).toBe(true);
    expect(getSession()?.role).toBe("ADMIN");
    expect(getPreviewMeta()).not.toBeNull();
    expect(readStashedAdminSession()).toBeNull();
  });

  it("drops orphan preview-meta when no usable Admin stash exists", () => {
    configureSessionPersistence(new MemoryPersistence());
    setPreviewMeta({
      previewId: previewResponse.previewId,
      previewExpiresAt: previewResponse.previewExpiresAt,
      temporaryCustomerId: previewResponse.temporaryCustomerId,
    });

    reconcilePreviewSessionOnHydrate();

    expect(getPreviewMeta()).toBeNull();
  });

  it("clears expired Admin stash and does not treat it as valid", () => {
    configureSessionPersistence(new MemoryPersistence());
    stashAdminSession({
      ...adminSession,
      expiresAt: "2020-01-01T00:00:00Z",
    });
    setPreviewMeta({
      previewId: previewResponse.previewId,
      previewExpiresAt: previewResponse.previewExpiresAt,
      temporaryCustomerId: previewResponse.temporaryCustomerId,
    });

    reconcilePreviewSessionOnHydrate();

    expect(readStashedAdminSession()).toBeNull();
    expect(getPreviewMeta()).toBeNull();
    expect(
      isExpired(
        { ...adminSession, expiresAt: "2020-01-01T00:00:00Z" },
        Date.parse("2099-01-01T00:00:00Z"),
      ),
    ).toBe(true);
  });
});
