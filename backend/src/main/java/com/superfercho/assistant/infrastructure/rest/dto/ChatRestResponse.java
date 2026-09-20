package com.superfercho.assistant.infrastructure.rest.dto;

import com.superfercho.assistant.application.confirmation.SensitiveActionType;
import com.superfercho.assistant.application.dto.chat.ChatResponse;
import java.util.UUID;

public record ChatRestResponse(
        UUID conversationId,
        String assistantMessage,
        boolean awaitingConfirmation,
        String confirmationToken,
        SensitiveActionType confirmationType) {

    public static ChatRestResponse from(ChatResponse response) {
        return new ChatRestResponse(
                response.conversationId(),
                response.assistantMessage(),
                response.awaitingConfirmation(),
                response.confirmationToken(),
                response.confirmationType());
    }
}
