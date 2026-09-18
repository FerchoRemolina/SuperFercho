package com.superfercho.knowledge.application.usecase;

import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.dto.KnowledgeSearchHit;
import com.superfercho.knowledge.application.dto.KnowledgeSearchResult;
import com.superfercho.knowledge.application.dto.SearchKnowledgeCommand;
import com.superfercho.knowledge.application.exception.InvalidSearchRequestException;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.application.port.EmbeddingPort;
import com.superfercho.knowledge.application.port.KnowledgeVectorStorePort;
import java.util.List;

public final class SearchKnowledgeUseCase {

    private static final int MAX_LIMIT = 20;

    private final EmbeddingPort embeddingPort;
    private final KnowledgeVectorStorePort vectorStore;

    public SearchKnowledgeUseCase(EmbeddingPort embeddingPort, KnowledgeVectorStorePort vectorStore) {
        this.embeddingPort = embeddingPort;
        this.vectorStore = vectorStore;
    }

    public KnowledgeSearchResult execute(SearchKnowledgeCommand command) {
        if (command.query() == null || command.query().isBlank()) {
            return new KnowledgeSearchResult(List.of());
        }
        if (command.limit() <= 0 || command.limit() > MAX_LIMIT) {
            throw new InvalidSearchRequestException("limit must be between 1 and " + MAX_LIMIT);
        }
        List<EmbeddingVector> embeddings = embedQuery(command.query());
        if (embeddings.size() != 1) {
            throw new KnowledgeProcessingException("embedding provider returned an invalid result");
        }
        return new KnowledgeSearchResult(search(embeddings.get(0), command.limit()));
    }

    private List<KnowledgeSearchHit> search(EmbeddingVector query, int limit) {
        try {
            List<KnowledgeSearchHit> hits = vectorStore.search(query, limit);
            return hits == null ? List.of() : hits;
        } catch (KnowledgeProcessingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new KnowledgeProcessingException("failed to search knowledge");
        }
    }

    private List<EmbeddingVector> embedQuery(String query) {
        try {
            List<EmbeddingVector> embeddings = embeddingPort.embed(List.of(query));
            if (embeddings == null) {
                throw new KnowledgeProcessingException("embedding provider returned an invalid result");
            }
            return embeddings;
        } catch (KnowledgeProcessingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new KnowledgeProcessingException("failed to embed search query");
        }
    }
}
