package com.superfercho.knowledge.application.dto;

import java.util.UUID;

public record ChunkEmbedding(UUID documentId, UUID chunkId, int position, EmbeddingVector embedding) {}
