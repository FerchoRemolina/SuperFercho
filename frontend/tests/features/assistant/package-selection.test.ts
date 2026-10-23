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
    expect(chat).toContain("packageProposal && !pendingConfirmation");
    expect(chat).not.toContain("confirmationTitle(packageProposal");
    expect(chat).not.toContain("confirmationConfirmLabel(packageProposal");
    expect(chat).toContain("Un «sí» por chat no agrega");
  });

  it("clears stale chips when a later response omits packageProposal", () => {
    expect(chat).toContain("packageProposalFromResponse(response)");
    expect(request).toContain("proposal.options.length === 0");
  });
});
