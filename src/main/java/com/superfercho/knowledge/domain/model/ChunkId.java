package com.superfercho.knowledge.domain.model;

import com.superfercho.knowledge.domain.exception.InvalidDocumentException;
import java.util.UUID;

public record ChunkId(UUID value) {

    public ChunkId {
        if (value == null) {
            throw new InvalidDocumentException("chunkId cannot be null");
        }
    }

    public static ChunkId generate() {
        return new ChunkId(UUID.randomUUID());
    }
}
