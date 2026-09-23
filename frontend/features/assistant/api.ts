import { request } from "@/shared/api/client";

/** Mirrors SensitiveActionType from Assistant REST. */
export type SensitiveActionType = "CHECKOUT" | "CANCEL_ORDER";

/** Mirrors ConfirmationRequest. */
export type ChatConfirmationRequest = {
  token: string;
};

/**
 * Mirrors ChatRequest. Identity comes from the JWT; never send userId.
 * Exactly one of message or confirmation is used per turn (backend rules).
 */
export type ChatRequest = {
  conversationId?: string;
  message?: string;
  confirmation?: ChatConfirmationRequest;
};

/** Mirrors ChatRestResponse. */
export type ChatRestResponse = {
  conversationId: string;
  assistantMessage: string;
  awaitingConfirmation: boolean;
  confirmationToken: string | null;
  confirmationType: SensitiveActionType | null;
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
