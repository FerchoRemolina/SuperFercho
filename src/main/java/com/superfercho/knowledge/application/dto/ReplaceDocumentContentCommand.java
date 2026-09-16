package com.superfercho.knowledge.application.dto;

import java.util.UUID;

public record ReplaceDocumentContentCommand(UUID documentId, String content) {}
