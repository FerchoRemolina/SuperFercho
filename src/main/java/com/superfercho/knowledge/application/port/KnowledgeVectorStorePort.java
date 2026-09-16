package com.superfercho.knowledge.application.port;

import com.superfercho.knowledge.application.dto.ChunkEmbedding;
import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.dto.KnowledgeSearchHit;
import java.util.List;
import java.util.UUID;

public interface KnowledgeVectorStorePort {

    void upsert(ChunkEmbedding embedding);

    void deleteByDocumentId(UUID documentId);

    List<KnowledgeSearchHit> search(EmbeddingVector query, int limit);
}
