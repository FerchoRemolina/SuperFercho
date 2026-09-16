package com.superfercho.knowledge.domain.model;

import com.superfercho.knowledge.domain.exception.InvalidDocumentException;

public record DocumentSource(String value) {

    public DocumentSource {
        if (value == null || value.isBlank()) {
            throw new InvalidDocumentException("source cannot be null or blank");
        }
    }
}
