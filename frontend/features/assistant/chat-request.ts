import type {
  ChatRequest,
  ChatRestResponse,
  PackageProposal,
} from "@/features/assistant/api";
import type { PendingConfirmation } from "@/features/assistant/presentation";

export function messageChatBody(
  conversationId: string | null,
  message: string,
): ChatRequest {
  return {
    ...(conversationId ? { conversationId } : {}),
    message,
  };
}

export function confirmationChatBody(
  conversationId: string,
  token: string,
): ChatRequest {
  return {
    conversationId,
    confirmation: { token },
  };
}

export function packageSelectionChatBody(
  conversationId: string,
  proposalId: string,
  packageCount: number,
): ChatRequest {
  return {
    conversationId,
    packageSelection: { proposalId, packageCount },
  };
}

export function pendingFromResponse(
  response: ChatRestResponse,
): PendingConfirmation | null {
  if (
    response.awaitingConfirmation &&
    response.confirmationToken &&
    response.confirmationType
  ) {
    return {
      token: response.confirmationToken,
      type: response.confirmationType,
    };
  }
  return null;
}

export function packageProposalIsCurrent(
  proposal: PackageProposal | null | undefined,
  nowMs: number = Date.now(),
): boolean {
  if (!proposal || !proposal.proposalId || proposal.options.length === 0) {
    return false;
  }
  if (!proposal.expiresAt) {
    return true;
  }
  const expires = Date.parse(proposal.expiresAt);
  return Number.isNaN(expires) || expires > nowMs;
}

export function packageProposalFromResponse(
  response: ChatRestResponse,
  nowMs: number = Date.now(),
): PackageProposal | null {
  if (!Object.prototype.hasOwnProperty.call(response, "packageProposal")) {
    return null;
  }
  const proposal = response.packageProposal;
  if (proposal == null || !packageProposalIsCurrent(proposal, nowMs)) {
    return null;
  }
  return proposal;
}
