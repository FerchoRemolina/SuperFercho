package com.superfercho.knowledge.domain.model;

import com.superfercho.knowledge.domain.exception.InvalidDocumentException;

public record ChunkPosition(int value) {

    public ChunkPosition {
        if (value < 0) {
            throw new InvalidDocumentException("chunk position must be greater than or equal to 0");
        }
    }
}
