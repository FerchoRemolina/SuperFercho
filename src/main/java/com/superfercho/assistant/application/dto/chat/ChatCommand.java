package com.superfercho.assistant.application.dto.chat;

import java.util.UUID;

public record ChatCommand(UUID conversationId, String message, ExplicitConfirmation confirmation) {}
