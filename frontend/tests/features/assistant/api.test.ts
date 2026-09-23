import { afterEach, describe, expect, it, vi } from "vitest";
import { postAssistantChat } from "@/features/assistant/api";
import {
  confirmationChatBody,
  messageChatBody,
  pendingFromResponse,
} from "@/features/assistant/chat-request";
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

describe("assistant api", () => {
  it("posts a first message to /assistant/chat with JWT and without userId", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          conversationId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
          assistantMessage: "Hola",
          awaitingConfirmation: false,
          confirmationToken: null,
          confirmationType: null,
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await postAssistantChat(messageChatBody(null, "Hola Fercho"));

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/assistant/chat");
    expect(init.method).toBe("POST");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer access-token",
    );
    const sent = JSON.parse(String(init.body)) as Record<string, unknown>;
    expect(sent).toEqual({ message: "Hola Fercho" });
    expect(sent).not.toHaveProperty("userId");
    expect(sent).not.toHaveProperty("conversationId");
  });

  it("keeps conversationId on follow-up messages", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          conversationId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
          assistantMessage: "Listo",
          awaitingConfirmation: false,
          confirmationToken: null,
          confirmationType: null,
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await postAssistantChat(
      messageChatBody(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        "¿Qué hay en mi carrito?",
      ),
    );

    const sent = JSON.parse(
      String((fetchMock.mock.calls[0] as [string, RequestInit])[1].body),
    ) as Record<string, unknown>;
    expect(sent).toEqual({
      conversationId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      message: "¿Qué hay en mi carrito?",
    });
  });

  it("sends confirmation token only through /assistant/chat", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          conversationId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
          assistantMessage: "Pedido confirmado",
          awaitingConfirmation: false,
          confirmationToken: null,
          confirmationType: null,
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await postAssistantChat(
      confirmationChatBody(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        "confirm-token",
      ),
    );

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://localhost:8080/api/v1/assistant/chat");
    expect(JSON.parse(String(init.body))).toEqual({
      conversationId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      confirmation: { token: "confirm-token" },
    });
  });

  it("surfaces LLM_PROVIDER_FAILED without inventing a fake assistant reply", async () => {
    configureSessionPersistence(new MemoryPersistence());
    setSession({
      userId: "11111111-1111-1111-1111-111111111111",
      role: "CUSTOMER",
      accessToken: "access-token",
      expiresAt: "2099-01-01T00:00:00Z",
    });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          status: 409,
          code: "LLM_PROVIDER_FAILED",
          title: "Conflict",
          detail: "LLM provider request failed",
        }),
        {
          status: 409,
          headers: { "Content-Type": "application/problem+json" },
        },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      postAssistantChat(messageChatBody(null, "Hola")),
    ).rejects.toMatchObject({
      problem: { status: 409, code: "LLM_PROVIDER_FAILED" },
    });
  });
});

describe("assistant chat request helpers", () => {
  it("detects awaitingConfirmation payloads", () => {
    expect(
      pendingFromResponse({
        conversationId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        assistantMessage: "¿Confirmas el pedido?",
        awaitingConfirmation: true,
        confirmationToken: "tok",
        confirmationType: "CHECKOUT",
      }),
    ).toEqual({ token: "tok", type: "CHECKOUT" });

    expect(
      pendingFromResponse({
        conversationId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        assistantMessage: "Listo",
        awaitingConfirmation: false,
        confirmationToken: null,
        confirmationType: null,
      }),
    ).toBeNull();
  });
});
