package com.superfercho.knowledge.domain.model;

import com.superfercho.knowledge.domain.exception.InvalidDocumentException;

public record ChunkText(String value) {

    public ChunkText {
        if (value == null || value.isBlank()) {
            throw new InvalidDocumentException("chunk text cannot be null or blank");
        }
    }
}
