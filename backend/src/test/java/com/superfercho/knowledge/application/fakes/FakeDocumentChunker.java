package com.superfercho.knowledge.application.fakes;

import com.superfercho.knowledge.application.port.DocumentChunkerPort;
import com.superfercho.knowledge.domain.model.ChunkText;
import com.superfercho.knowledge.domain.model.DocumentContent;
import java.util.ArrayList;
import java.util.List;

public final class FakeDocumentChunker implements DocumentChunkerPort {

    private final List<DocumentContent> calls = new ArrayList<>();
    private List<ChunkText> chunks = List.of();
    private boolean configured;
    private RuntimeException failure;

    public void succeedWith(String... texts) {
        this.failure = null;
        this.configured = true;
        List<ChunkText> next = new ArrayList<>();
        for (String text : texts) {
            next.add(new ChunkText(text));
        }
        this.chunks = List.copyOf(next);
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public List<DocumentContent> calls() {
        return List.copyOf(calls);
    }

    @Override
    public List<ChunkText> chunk(DocumentContent content) {
        calls.add(content);
        if (failure != null) {
            throw failure;
        }
        if (!configured && content != null) {
            return List.of(new ChunkText(content.value()));
        }
        return chunks;
    }
}
