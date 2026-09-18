package com.superfercho.knowledge.domain.model;

public record KnowledgeChunkSnapshot(ChunkId id, ChunkPosition position, ChunkText text, boolean embedded) {}
