import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("package selection chips", () => {
  const chat = source("features/assistant/components/assistant-chat.tsx");
  const request = source("features/assistant/chat-request.ts");
  const header = source("features/assistant/components/fercho-panel-header.tsx");
  const host = source("features/assistant/components/fercho-host.tsx");

  it("renders chips from packageProposal and sends proposalId with packageCount", () => {
    expect(chat).toContain("packageProposalFromResponse");
    expect(chat).toContain("setPackageProposal");
    expect(chat).toContain("packageSelectionChatBody");
    expect(chat).toContain("packageProposal.proposalId");
    expect(chat).toContain("option.packageCount");
    expect(chat).toContain("packageOptionLabel");
    expect(request).toContain("packageSelection: { proposalId, packageCount }");
  });

  it("does not reuse checkout or cancellation confirmation UI for chips", () => {
    expect(chat).toContain("packageProposal && proposalIsLive && !pendingConfirmation");
    expect(chat).not.toContain("confirmationTitle(packageProposal");
    expect(chat).not.toContain("confirmationConfirmLabel(packageProposal");
    expect(chat).toContain("Un «sí» por chat no agrega");
  });

  it("clears stale chips when a later response omits packageProposal", () => {
    expect(chat).toContain("setPackageProposal(packageProposalFromResponse(response))");
    expect(request).toContain('hasOwnProperty.call(response, "packageProposal")');
    expect(request).toContain("packageProposalIsCurrent");
  });

  it("Nueva conversación drops local chips without posting a selection", () => {
    expect(chat).toContain("function resetConversation");
    expect(chat).toContain("setConversationId(null)");
    expect(chat).toContain("setPackageProposal(null)");
    expect(chat).toContain("onNewConversation={resetConversation}");
    expect(chat).not.toContain("resetConversation(); void postAssistantChat");
  });

  it("minimizing or expanding Fercho keeps a live proposal", () => {
    expect(chat).toContain("onMinimize={onMinimize}");
    expect(chat).toContain("onToggleExpanded={onToggleExpanded}");
    expect(chat).not.toContain("onMinimize={resetConversation}");
    expect(chat).not.toContain("onToggleExpanded={resetConversation}");
    expect(header).not.toContain("setPackageProposal");
    expect(header).not.toContain("postAssistantChat");
    expect(host).not.toContain("setPackageProposal");
    expect(host).not.toContain("postAssistantChat");
  });
});
