package com.superfercho.knowledge.domain.model;

import com.superfercho.knowledge.domain.exception.InvalidDocumentException;

public record DocumentTitle(String value) {

    public DocumentTitle {
        if (value == null || value.isBlank()) {
            throw new InvalidDocumentException("title cannot be null or blank");
        }
    }
}
