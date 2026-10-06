package com.superfercho.assistant.infrastructure.rest.dto;

import java.util.UUID;

public record ClaimConversationRestResponse(UUID conversationId) {

    public static ClaimConversationRestResponse from(UUID conversationId) {
        return new ClaimConversationRestResponse(conversationId);
    }
}
