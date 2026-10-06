package com.superfercho.assistant.application.dto.chat;

import java.util.UUID;

public record ClaimConversationCommand(UUID conversationId, String visitorToken) {}
