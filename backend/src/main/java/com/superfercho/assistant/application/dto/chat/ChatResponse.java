package com.superfercho.assistant.application.dto.chat;

import com.superfercho.assistant.application.confirmation.SensitiveActionType;
import java.util.UUID;

public record ChatResponse(
        UUID conversationId,
        String assistantMessage,
        boolean awaitingConfirmation,
        String confirmationToken,
        SensitiveActionType confirmationType) {}
