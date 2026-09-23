import type { ChatRequest, ChatRestResponse } from "@/features/assistant/api";
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
