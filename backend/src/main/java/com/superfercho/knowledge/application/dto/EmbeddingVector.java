package com.superfercho.knowledge.application.dto;

import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import java.util.Arrays;

public record EmbeddingVector(float[] values) {

    public EmbeddingVector {
        if (values == null || values.length == 0) {
            throw new KnowledgeProcessingException("embedding cannot be null or empty");
        }
        values = values.clone();
    }

    @Override
    public float[] values() {
        return values.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EmbeddingVector vector && Arrays.equals(values, vector.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "EmbeddingVector[length=" + values.length + "]";
    }
}
