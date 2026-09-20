import { afterEach, describe, expect, it } from "vitest";
import {
  SESSION_STORAGE_KEY,
  clearSession,
  configureSessionPersistence,
  getAccessToken,
  getSession,
  hydrateSession,
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

const validSession = {
  userId: "11111111-1111-1111-1111-111111111111",
  role: "CUSTOMER" as const,
  accessToken: "token-value",
  expiresAt: "2099-01-01T00:00:00Z",
};

afterEach(() => {
  clearSession();
  configureSessionPersistence(null);
});

describe("session store", () => {
  it("persists a session and restores it on hydrate", () => {
    const store = new MemoryPersistence();
    configureSessionPersistence(store);

    setSession(validSession);

    const stored = store.getItem(SESSION_STORAGE_KEY);
    expect(stored).toContain("token-value");

    clearSession();
    expect(getSession()).toBeNull();
    if (stored) {
      store.setItem(SESSION_STORAGE_KEY, stored);
    }

    const restored = hydrateSession();
    expect(restored).toEqual(validSession);
    expect(getAccessToken()).toBe("token-value");
  });

  it("clears an expired session instead of restoring it", () => {
    const store = new MemoryPersistence();
    configureSessionPersistence(store);
    store.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({
        ...validSession,
        expiresAt: "2020-01-01T00:00:00Z",
      }),
    );

    expect(hydrateSession()).toBeNull();
    expect(getAccessToken()).toBeNull();
    expect(store.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });
});
