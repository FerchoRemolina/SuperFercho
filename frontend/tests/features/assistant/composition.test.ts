import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("assistant composition", () => {
  it("replaces the assistant placeholder with the real chat UI", () => {
    const page = source("app/(customer)/assistant/page.tsx");
    expect(page).toContain("AssistantChat");
    expect(page).not.toContain("PlaceholderScreen");
    expect(page).not.toContain("aún no está disponible");
  });

  it("routes chat mutations only through POST /assistant/chat", () => {
    const api = source("features/assistant/api.ts");
    const chat = source("features/assistant/components/assistant-chat.tsx");
    const chatRequest = source("features/assistant/chat-request.ts");

    expect(api).toContain('"/assistant/chat"');
    expect(api).toContain('method: "POST"');
    expect(chat).toContain("postAssistantChat");
    expect(chat).toContain("confirmationChatBody");
    expect(chat).toContain("messageChatBody");
    expect(chat).not.toContain('"/orders"');
    expect(chat).not.toContain("/cancel");
    expect(chat).not.toContain("checkout(");
    expect(chat).not.toContain("cancelOrder(");
    expect(chatRequest).not.toContain("userId");
    expect(api).toContain("never send userId");
  });

  it("blocks duplicate sends while pending and shows loading copy", () => {
    const chat = source("features/assistant/components/assistant-chat.tsx");
    expect(chat).toContain("Fercho está pensando");
    expect(chat).toContain("sending || pendingConfirmation");
    expect(chat).toContain("pendingConfirmation !== null");
    expect(chat).toContain("Nueva conversación");
  });

  it("handles awaitingConfirmation with Assistant confirmation only", () => {
    const chat = source("features/assistant/components/assistant-chat.tsx");
    expect(chat).toContain("pendingFromResponse");
    expect(chat).toContain("confirmationChatBody(conversationId, pendingConfirmation.token)");
    expect(chat).toContain("dismissConfirmation");
    expect(chat).toContain("confirmationTitle");
  });

  it("invalidates existing cart, orders, lists and addresses query roots", () => {
    const invalidate = source("features/assistant/invalidate.ts");
    expect(invalidate).toContain("cartKeys().root()");
    expect(invalidate).toContain("orderKeys().all");
    expect(invalidate).toContain("shoppingListsKeys().root()");
    expect(invalidate).toContain("addressKeys().root()");
    expect(invalidate).not.toContain('["assistant"');
  });

  it("exposes Fercho in customer navigation and home without unavailable copy", () => {
    const header = source("shared/ui/site-header.tsx");
    const home = source("app/page.tsx");
    expect(header).toContain('href="/assistant"');
    expect(header).toContain("Fercho");
    expect(home).toContain('href="/assistant"');
    expect(home).toContain("Hablar con Fercho");
    expect(home).not.toContain("aún no");
    expect(home).not.toContain("no está disponible");
  });
});
