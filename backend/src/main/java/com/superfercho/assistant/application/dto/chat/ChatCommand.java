package com.superfercho.assistant.application.dto.chat;

import java.util.UUID;

public record ChatCommand(
        UUID conversationId, String message, ExplicitConfirmation confirmation, String visitorToken) {

    public ChatCommand(UUID conversationId, String message, ExplicitConfirmation confirmation) {
        this(conversationId, message, confirmation, null);
    }
}
