package com.superfercho.knowledge.application.port;

import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeDocumentRepository {

    KnowledgeDocument save(KnowledgeDocument document);

    Optional<KnowledgeDocument> findById(UUID id);

    List<KnowledgeDocument> findAll();
}
