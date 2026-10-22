import { afterEach, describe, expect, it, vi } from "vitest";
import {
  addAddress,
  addressKeys,
  customerProfileKeys,
  deactivateAddress,
  getCustomerProfile,
  listAddresses,
  setDefaultAddress,
  updateAddress,
  updateCustomerProfile,
  type Address,
  type CustomerProfile,
} from "@/features/account/api";
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

const address: Address = {
  id: "22222222-2222-2222-2222-222222222222",
  label: "Casa",
  recipientName: "Ada Lovelace",
  addressLine: "Calle 1 # 2-3",
  additionalInfo: "Apto 101",
  city: "Bogotá",
  department: "Cundinamarca",
  phone: "3001234567",
  isDefault: true,
  status: "ACTIVE",
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

describe("addresses api", () => {
  it("sends the JWT on GET /addresses and never a customerId", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([address]));
    vi.stubGlobal("fetch", fetchMock);

    await expect(listAddresses()).resolves.toEqual([address]);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/addresses");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer access-token",
    );
    expect(init.body).toBeUndefined();
  });

  it("posts real AddAddressRequest fields including isDefault", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(address, 201));
    vi.stubGlobal("fetch", fetchMock);

    await addAddress({
      label: "Casa",
      recipientName: "Ada Lovelace",
      addressLine: "Calle 1 # 2-3",
      additionalInfo: "Apto 101",
      city: "Bogotá",
      department: "Cundinamarca",
      phone: "3001234567",
      isDefault: true,
    });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/addresses");
    expect(init.method).toBe("POST");
    expect(init.body).toBe(
      JSON.stringify({
        label: "Casa",
        recipientName: "Ada Lovelace",
        addressLine: "Calle 1 # 2-3",
        additionalInfo: "Apto 101",
        city: "Bogotá",
        department: "Cundinamarca",
        phone: "3001234567",
        isDefault: true,
      }),
    );
    expect(String(init.body)).not.toContain("customerId");
  });

  it("puts update fields without isDefault", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(address));
    vi.stubGlobal("fetch", fetchMock);

    await updateAddress(address.id, {
      label: "Oficina",
      recipientName: "Ada Lovelace",
      addressLine: "Calle 9 # 8-7",
      additionalInfo: "Piso 2",
      city: "Medellín",
      department: "Antioquia",
      phone: "3009876543",
    });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      "http://localhost:8080/api/v1/addresses/22222222-2222-2222-2222-222222222222",
    );
    expect(init.method).toBe("PUT");
    expect(init.body).toBe(
      JSON.stringify({
        label: "Oficina",
        recipientName: "Ada Lovelace",
        addressLine: "Calle 9 # 8-7",
        additionalInfo: "Piso 2",
        city: "Medellín",
        department: "Antioquia",
        phone: "3009876543",
      }),
    );
    expect(String(init.body)).not.toContain("isDefault");
  });

  it("deactivates with DELETE and sets default with POST /default", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ ...address, status: "INACTIVE", isDefault: false }))
      .mockResolvedValueOnce(jsonResponse(address));
    vi.stubGlobal("fetch", fetchMock);

    await deactivateAddress(address.id);
    await setDefaultAddress(address.id);

    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/addresses/22222222-2222-2222-2222-222222222222",
    );
    expect((fetchMock.mock.calls[0]?.[1] as RequestInit).method).toBe("DELETE");
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      "http://localhost:8080/api/v1/addresses/22222222-2222-2222-2222-222222222222/default",
    );
    expect((fetchMock.mock.calls[1]?.[1] as RequestInit).method).toBe("POST");
  });

  it("uses a single query key for addresses", () => {
    expect(addressKeys().root()).toEqual(["addresses"]);
  });
});

describe("customer profile api", () => {
  const profile: CustomerProfile = {
    id: "11111111-1111-1111-1111-111111111111",
    documentType: "CC",
    documentNumber: "12345678",
    firstName: "Ada",
    lastName: "Lovelace",
    email: "ada@example.com",
    phone: "3001234567",
    createdAt: "2026-01-15T12:00:00Z",
    updatedAt: "2026-04-01T15:00:00Z",
  };

  function customerSession(): void {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
  }

  it("gets the own profile with GET /customers/me and the JWT", async () => {
    customerSession();
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(profile));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getCustomerProfile()).resolves.toEqual(profile);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/customers/me");
    expect(init.method).toBe("GET");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer access-token",
    );
    expect(init.body).toBeUndefined();
  });

  it("puts exactly the six editable fields without identifiers", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(profile));
    vi.stubGlobal("fetch", fetchMock);

    await updateCustomerProfile({
      documentType: "CE",
      documentNumber: "987654321",
      firstName: "Ada Clará",
      lastName: "Clará",
      email: "updated@example.com",
      phone: "3112223344",
    });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/customers/me");
    expect(init.method).toBe("PUT");
    expect(init.body).toBe(
      JSON.stringify({
        documentType: "CE",
        documentNumber: "987654321",
        firstName: "Ada Clará",
        lastName: "Clará",
        email: "updated@example.com",
        phone: "3112223344",
      }),
    );
    expect(String(init.body)).not.toContain("userId");
    expect(String(init.body)).not.toContain("password");
    expect(String(init.body)).not.toContain("role");
  });

  it("propagates duplicate email conflicts as ApiError", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: "USER_ALREADY_EXISTS" }), {
        status: 409,
        headers: { "Content-Type": "application/problem+json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updateCustomerProfile({
        documentType: "CC",
        documentNumber: "12345678",
        firstName: "Ada",
        lastName: "Lovelace",
        email: "taken@example.com",
        phone: "3001234567",
      }),
    ).rejects.toMatchObject({
      problem: { status: 409, code: "USER_ALREADY_EXISTS" },
    });
  });

  it("uses a single query key for the customer profile", () => {
    expect(customerProfileKeys().root()).toEqual(["customer-profile"]);
  });
});
