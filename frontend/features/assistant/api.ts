import { request } from "@/shared/api/client";

/** Mirrors SensitiveActionType from Assistant REST. */
export type SensitiveActionType = "CHECKOUT" | "CANCEL_ORDER";

/** Mirrors ConfirmationRequest. */
export type ChatConfirmationRequest = {
  token: string;
};

/** Mirrors PackageSelectionRequest. Independent of confirmation. */
export type PackageSelectionRequest = {
  proposalId: string;
  packageCount: number;
};

export type PackageProposalOption = {
  packageCount: number;
  physicalTotalBase: string | number;
  baseUnit: string;
};

export type PackageProposal = {
  proposalId: string;
  productId: string;
  productName: string;
  presentationQuantity: string | number;
  presentationUnit: string;
  requestedQuantity: string | number;
  requestedUnit: string;
  options: PackageProposalOption[];
};

/**
 * Mirrors ChatRequest. Identity comes from the JWT; never send userId.
 * Exactly one of message, confirmation, or packageSelection is used per turn
 * (backend rules). confirmation+message remains allowed as a legacy pair.
 */
export type ChatRequest = {
  conversationId?: string;
  message?: string;
  confirmation?: ChatConfirmationRequest;
  packageSelection?: PackageSelectionRequest;
};

/** Mirrors ChatRestResponse. */
export type ChatRestResponse = {
  conversationId: string;
  assistantMessage: string;
  awaitingConfirmation: boolean;
  confirmationToken: string | null;
  confirmationType: SensitiveActionType | null;
  packageProposal?: PackageProposal | null;
};

/** POST /api/v1/assistant/chat — sole Agent entry point for chat actions. */
export async function postAssistantChat(
  body: ChatRequest,
): Promise<ChatRestResponse> {
  return request<ChatRestResponse>("/assistant/chat", {
    method: "POST",
    body,
  });
}
