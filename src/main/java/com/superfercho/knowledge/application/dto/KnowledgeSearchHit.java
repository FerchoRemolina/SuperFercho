package com.superfercho.knowledge.application.dto;

import java.util.UUID;

public record KnowledgeSearchHit(
        UUID documentId, UUID chunkId, String title, String source, String chunkText, double score) {}
