package com.superfercho.knowledge.domain.model;

import com.superfercho.knowledge.domain.exception.InvalidDocumentException;
import java.util.UUID;

public record DocumentId(UUID value) {

    public DocumentId {
        if (value == null) {
            throw new InvalidDocumentException("id cannot be null");
        }
    }

    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID());
    }
}
