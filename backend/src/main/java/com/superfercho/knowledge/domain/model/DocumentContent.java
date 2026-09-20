package com.superfercho.knowledge.domain.model;

import com.superfercho.knowledge.domain.exception.InvalidDocumentException;

public record DocumentContent(String value) {

    public DocumentContent {
        if (value == null || value.isBlank()) {
            throw new InvalidDocumentException("content cannot be null or blank");
        }
    }
}
