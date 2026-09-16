package com.superfercho.knowledge.application.fakes;

import com.superfercho.knowledge.application.dto.ChunkEmbedding;
import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.dto.KnowledgeSearchHit;
import com.superfercho.knowledge.application.port.KnowledgeVectorStorePort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FakeKnowledgeVectorStore implements KnowledgeVectorStorePort {

    private final Map<UUID, List<ChunkEmbedding>> byDocument = new LinkedHashMap<>();
    private final List<ChunkEmbedding> upserts = new ArrayList<>();
    private final List<UUID> deletes = new ArrayList<>();
    private final List<SearchCall> searches = new ArrayList<>();
    private List<KnowledgeSearchHit> hits = List.of();
    private RuntimeException upsertFailure;
    private int failOnUpsertNumber;
    private RuntimeException deleteFailure;

    public void failOnUpsert(int callNumber, RuntimeException failure) {
        this.failOnUpsertNumber = callNumber;
        this.upsertFailure = failure;
    }

    public void failOnDelete(RuntimeException failure) {
        this.deleteFailure = failure;
    }

    public void setHits(List<KnowledgeSearchHit> hits) {
        this.hits = List.copyOf(hits);
    }

    public List<ChunkEmbedding> upserts() {
        return List.copyOf(upserts);
    }

    public List<UUID> deletes() {
        return List.copyOf(deletes);
    }

    public List<SearchCall> searches() {
        return List.copyOf(searches);
    }

    public List<ChunkEmbedding> embeddingsOf(UUID documentId) {
        return List.copyOf(byDocument.getOrDefault(documentId, List.of()));
    }

    @Override
    public void upsert(ChunkEmbedding embedding) {
        if (upsertFailure != null && upserts.size() + 1 == failOnUpsertNumber) {
            throw upsertFailure;
        }
        upserts.add(embedding);
        byDocument.computeIfAbsent(embedding.documentId(), key -> new ArrayList<>()).add(embedding);
    }

    @Override
    public void deleteByDocumentId(UUID documentId) {
        if (deleteFailure != null) {
            throw deleteFailure;
        }
        deletes.add(documentId);
        byDocument.remove(documentId);
    }

    @Override
    public List<KnowledgeSearchHit> search(EmbeddingVector query, int limit) {
        searches.add(new SearchCall(query, limit));
        return hits;
    }

    public record SearchCall(EmbeddingVector query, int limit) {}
}
