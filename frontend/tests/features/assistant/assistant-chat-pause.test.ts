import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("fercho pause handling in the chat", () => {
  const chat = source("features/assistant/components/assistant-chat.tsx");
  const messages = source("shared/errors/messages.ts");
  const apiProblem = source("shared/errors/api-problem.ts");

  it("maps the backend pause, suspension and unavailability codes", () => {
    expect(messages).toContain('case "FERCHO_PAUSED"');
    expect(messages).toContain('case "FERCHO_SUSPENDED"');
    expect(messages).toContain('case "FERCHO_UNAVAILABLE"');
    expect(messages).toContain("me estoy cansando");
    expect(messages).toContain("suspendido temporalmente");
  });

  it("surfaces problem properties for the server-side countdown", () => {
    expect(apiProblem).toContain("properties");
    expect(chat).toContain("remainingSeconds");
    expect(chat).toContain("pausedUntil");
  });

  it("blocks the send button while the pause countdown is active", () => {
    expect(chat).toContain("pauseSecondsLeft !== null && pauseSecondsLeft > 0");
  });

  it("clears the countdown timer on unmount and re-render", () => {
    expect(chat).toContain("clearInterval");
  });

  it("keeps the rest of the shop available during a pause", () => {
    expect(chat).toContain("El resto de la tienda sigue funcionando con normalidad");
  });

  it("does not delete the thread or data when Fercho is paused", () => {
    expect(chat).not.toContain("setMessages([]);\n    setConversationId(null);\n    setPause(null)");
    expect(chat).not.toContain("clearCart");
    expect(chat).not.toContain("logout");
  });
});
