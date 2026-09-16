package com.superfercho.knowledge.domain.model;

import com.superfercho.knowledge.domain.exception.InvalidDocumentException;

public final class KnowledgeChunk {

    private final ChunkId id;
    private final ChunkPosition position;
    private final ChunkText text;
    private final boolean embedded;

    private KnowledgeChunk(ChunkId id, ChunkPosition position, ChunkText text, boolean embedded) {
        this.id = id;
        this.position = position;
        this.text = text;
        this.embedded = embedded;
    }

    static KnowledgeChunk create(ChunkId id, ChunkPosition position, ChunkText text) {
        return of(id, position, text, false);
    }

    static KnowledgeChunk reconstitute(ChunkId id, ChunkPosition position, ChunkText text, boolean embedded) {
        return of(id, position, text, embedded);
    }

    KnowledgeChunk markEmbedded() {
        if (embedded) {
            return this;
        }
        return of(id, position, text, true);
    }

    public ChunkId id() {
        return id;
    }

    public ChunkPosition position() {
        return position;
    }

    public ChunkText text() {
        return text;
    }

    public boolean embedded() {
        return embedded;
    }

    private static KnowledgeChunk of(ChunkId id, ChunkPosition position, ChunkText text, boolean embedded) {
        requireNonNull(id, "chunkId");
        requireNonNull(position, "chunk position");
        requireNonNull(text, "chunk text");
        return new KnowledgeChunk(id, position, text, embedded);
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidDocumentException(field + " cannot be null");
        }
    }
}
