package com.superfercho.knowledge.application.fakes;

import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryKnowledgeDocumentRepository implements KnowledgeDocumentRepository {

    private final Map<UUID, KnowledgeDocument> documents = new LinkedHashMap<>();

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        documents.put(document.id().value(), document);
        return document;
    }

    @Override
    public Optional<KnowledgeDocument> findById(UUID id) {
        return Optional.ofNullable(documents.get(id));
    }

    @Override
    public List<KnowledgeDocument> findAll() {
        return List.copyOf(documents.values());
    }

    public List<KnowledgeDocument> saved() {
        return new ArrayList<>(documents.values());
    }
}
