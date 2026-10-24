import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("fercho panel header chrome", () => {
  const header = source("features/assistant/components/fercho-panel-header.tsx");
  const chat = source("features/assistant/components/assistant-chat.tsx");
  const fab = source("features/assistant/components/fercho-fab.tsx");
  const host = source("features/assistant/components/fercho-host.tsx");

  it("does not keep a permanent Nueva conversación label on the plus control", () => {
    expect(header).toContain("PlusIcon");
    expect(header).toContain("tooltip=\"Nueva conversación\"");
    expect(header).toContain("size-11");
    expect(header).not.toContain("variant=\"secondary\"");
  });

  it("wires new conversation to the existing local reset", () => {
    expect(chat).toContain("onNewConversation={resetConversation}");
    expect(chat).toContain("function resetConversation");
    expect(chat).toContain("setConversationId(null)");
    expect(chat).toContain("setMessages([])");
    expect(chat).toContain("setPackageProposal(null)");
  });

  it("minimizes to the FAB without cart or chat posts", () => {
    expect(header).toContain('label="Minimizar"');
    expect(header).toContain("MinusIcon");
    expect(fab).toContain("toggle");
    expect(fab).toContain("aria-controls=\"fercho-shell\"");
    expect(host).toContain('!showUi && "hidden"');
    expect(header).not.toContain("postAssistantChat");
    expect(header).not.toContain("addProductToCart");
  });
});
