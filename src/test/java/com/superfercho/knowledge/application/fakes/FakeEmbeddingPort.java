package com.superfercho.knowledge.application.fakes;

import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.port.EmbeddingPort;
import java.util.ArrayList;
import java.util.List;

public final class FakeEmbeddingPort implements EmbeddingPort {

    private final List<List<String>> calls = new ArrayList<>();
    private int failOnCallNumber;
    private RuntimeException failure;

    public void failOnCall(int callNumber, RuntimeException failure) {
        this.failOnCallNumber = callNumber;
        this.failure = failure;
    }

    public void succeed() {
        this.failOnCallNumber = 0;
        this.failure = null;
    }

    public List<List<String>> calls() {
        return List.copyOf(calls);
    }

    @Override
    public List<EmbeddingVector> embed(List<String> texts) {
        calls.add(List.copyOf(texts));
        if (failure != null && calls.size() == failOnCallNumber) {
            throw failure;
        }
        List<EmbeddingVector> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(new EmbeddingVector(new float[] {text.hashCode()}));
        }
        return embeddings;
    }
}
