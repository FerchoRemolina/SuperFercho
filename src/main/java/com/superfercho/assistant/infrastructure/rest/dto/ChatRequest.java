package com.superfercho.assistant.infrastructure.rest.dto;

import java.util.UUID;

public record ChatRequest(UUID conversationId, String message, ConfirmationRequest confirmation) {}
