import { afterEach, describe, expect, it } from "vitest";
import { applyCustomerProfileNamesToSession } from "@/features/account/profile-session";
import {
  SESSION_STORAGE_KEY,
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

function customerSession(): void {
  configureSessionPersistence(new MemoryPersistence());
  setSession({
    userId: "11111111-1111-1111-1111-111111111111",
    role: "CUSTOMER",
    accessToken: "access-token",
    expiresAt: "2099-01-01T00:00:00Z",
  });
}

afterEach(() => {
  clearSession();
  configureSessionPersistence(null);
});

describe("applyCustomerProfileNamesToSession", () => {
  it("updates names only after success and preserves credentials and role", () => {
    customerSession();

    applyCustomerProfileNamesToSession("Ada Clará", "Clará");

    const session = getSession();
    expect(session?.firstName).toBe("Ada Clará");
    expect(session?.lastName).toBe("Clará");
    expect(session?.userId).toBe("11111111-1111-1111-1111-111111111111");
    expect(session?.role).toBe("CUSTOMER");
    expect(session?.accessToken).toBe("access-token");
    expect(session?.expiresAt).toBe("2099-01-01T00:00:00Z");
  });

  it("persists the refreshed names", () => {
    const persistence = new MemoryPersistence();
    configureSessionPersistence(persistence);
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });

    applyCustomerProfileNamesToSession("Ada Clará", "Clará");

    const raw = persistence.getItem(SESSION_STORAGE_KEY);
    expect(raw).not.toBeNull();
    const parsed = JSON.parse(raw as string) as Record<string, unknown>;
    expect(parsed.firstName).toBe("Ada Clará");
    expect(parsed.lastName).toBe("Clará");
    expect(parsed.accessToken).toBe("access-token");
    expect(parsed.role).toBe("CUSTOMER");
  });

  it("omits blank names instead of storing empty strings", () => {
    customerSession();

    applyCustomerProfileNamesToSession("   ", "");

    const session = getSession();
    expect(session?.firstName).toBeUndefined();
    expect(session?.lastName).toBeUndefined();
  });

  it("does nothing without an active session", () => {
    configureSessionPersistence(new MemoryPersistence());

    applyCustomerProfileNamesToSession("Ada", "Lovelace");

    expect(getSession()).toBeNull();
  });

  it("does nothing for a non CUSTOMER session", () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "22222222-2222-2222-2222-222222222222",
      role: "ADMIN",
      accessToken: "admin-token",
      expiresAt: "2099-01-01T00:00:00Z",
      firstName: "Admin",
    });

    applyCustomerProfileNamesToSession("Hacker", "Name");

    expect(getSession()?.firstName).toBe("Admin");
  });
});
