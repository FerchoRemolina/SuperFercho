import { afterEach, describe, expect, it, vi } from "vitest";
import {
  homePathForRole,
  landingPathForRole,
  login,
  REGISTER_DOCUMENT_TYPES,
  registerCustomer,
  requestPasswordRecovery,
  resetPassword,
  sessionFromAuthentication,
} from "@/features/auth/api";
import { FORGOT_PASSWORD_PATH } from "@/features/auth/password-recovery-presentation";
import { clearSession } from "@/shared/session/session";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  clearSession();
});

describe("auth api", () => {
  it("posts login credentials and maps the authentication response to a session", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          userId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
          role: "CUSTOMER",
          firstName: "Ada",
          lastName: "Lovelace",
          accessToken: "jwt-token",
          expiresAt: "2026-03-01T11:00:00Z",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    const response = await login({
      email: "ada@identity.test",
      password: "secret-password",
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/auth/login");
    expect(init.method).toBe("POST");
    expect(init.body).toBe(
      JSON.stringify({
        email: "ada@identity.test",
        password: "secret-password",
      }),
    );
    expect(sessionFromAuthentication(response)).toEqual({
      userId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      role: "CUSTOMER",
      firstName: "Ada",
      lastName: "Lovelace",
      accessToken: "jwt-token",
      expiresAt: "2026-03-01T11:00:00Z",
    });
  });

  it("posts the seven register fields and does not treat the 201 as a session", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          id: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
          documentType: "CC",
          documentNumber: "123",
          firstName: "Ada",
          lastName: "Lovelace",
          email: "ada@identity.test",
          phone: "3001234567",
          role: "CUSTOMER",
          status: "ACTIVE",
          createdAt: "2026-03-01T11:00:00Z",
        }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    const created = await registerCustomer({
      documentType: "CC",
      documentNumber: "123",
      firstName: "Ada",
      lastName: "Lovelace",
      email: "ada@identity.test",
      phone: "3001234567",
      password: "secret-password",
    });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/customers");
    expect(init.method).toBe("POST");
    expect(JSON.parse(String(init.body))).toEqual({
      documentType: "CC",
      documentNumber: "123",
      firstName: "Ada",
      lastName: "Lovelace",
      email: "ada@identity.test",
      phone: "3001234567",
      password: "secret-password",
    });
    expect(created.role).toBe("CUSTOMER");
    expect(created).not.toHaveProperty("accessToken");
  });

  it("exposes only documentType values that Identity tests already send", () => {
    expect(REGISTER_DOCUMENT_TYPES.map((type) => type.value)).toEqual([
      "CC",
      "CE",
    ]);
  });

  it("sends ADMIN to /admin and CUSTOMER to home", () => {
    expect(homePathForRole("ADMIN")).toBe("/admin");
    expect(homePathForRole("CUSTOMER")).toBe("/");
  });

  it("routes the post-login landing by role without breaking valid next paths", () => {
    expect(landingPathForRole("ADMIN", null)).toBe("/admin");
    expect(landingPathForRole("ADMIN", undefined)).toBe("/admin");
    expect(landingPathForRole("CUSTOMER", null)).toBe("/");
    expect(landingPathForRole("CUSTOMER", "/cart")).toBe("/cart");
    expect(landingPathForRole("ADMIN", "/admin/orders")).toBe("/admin/orders");
    expect(landingPathForRole("ADMIN", "/cart")).toBe("/admin");
    expect(landingPathForRole("ADMIN", "/")).toBe("/admin");
    expect(landingPathForRole("CUSTOMER", "/admin/orders")).toBe("/");
    expect(landingPathForRole("ADMIN", "https://externo.test")).toBe("/admin");
  });

  it("posts password recovery anonymously and returns the generic message", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          message:
            "Si existe una cuenta asociada a este correo, recibirás un enlace para restablecer tu contraseña.",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    const response = await requestPasswordRecovery({
      email: "ada@identity.test",
    });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/auth/password-recovery");
    expect(init.method).toBe("POST");
    expect(JSON.parse(String(init.body))).toEqual({
      email: "ada@identity.test",
    });
    expect(new Headers(init.headers).get("Authorization")).toBeNull();
    expect(response.message).toContain("Si existe una cuenta asociada");
    expect(FORGOT_PASSWORD_PATH).toBe("/forgot-password");
  });

  it("posts password reset with token and passwords as anonymous 204", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await resetPassword({
      token: "raw-token",
      newPassword: "Luis123!",
      confirmPassword: "Luis123!",
    });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      "http://localhost:8080/api/v1/auth/password-recovery/reset",
    );
    expect(init.method).toBe("POST");
    expect(JSON.parse(String(init.body))).toEqual({
      token: "raw-token",
      newPassword: "Luis123!",
      confirmPassword: "Luis123!",
    });
  });
});
