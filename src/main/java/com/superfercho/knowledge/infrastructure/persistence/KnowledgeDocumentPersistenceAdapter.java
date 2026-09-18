package com.superfercho.knowledge.infrastructure.persistence;

import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import com.superfercho.knowledge.infrastructure.persistence.entity.KnowledgeDocumentJpaEntity;
import com.superfercho.knowledge.infrastructure.persistence.mapper.KnowledgeDocumentPersistenceMapper;
import com.superfercho.knowledge.infrastructure.persistence.repository.KnowledgeDocumentJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("!test")
public class KnowledgeDocumentPersistenceAdapter implements KnowledgeDocumentRepository {

    private final KnowledgeDocumentJpaRepository documentJpaRepository;
    private final KnowledgeDocumentPersistenceMapper documentPersistenceMapper;
    private final TransactionTemplate transactionTemplate;

    public KnowledgeDocumentPersistenceAdapter(
            KnowledgeDocumentJpaRepository documentJpaRepository,
            KnowledgeDocumentPersistenceMapper documentPersistenceMapper,
            PlatformTransactionManager transactionManager) {
        this.documentJpaRepository = documentJpaRepository;
        this.documentPersistenceMapper = documentPersistenceMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        KnowledgeDocumentJpaEntity incoming = documentPersistenceMapper.toEntity(document);
        return transactionTemplate.execute(status -> documentJpaRepository
                .findById(incoming.getId())
                .map(existing -> replaceExisting(existing, incoming))
                .orElseGet(() -> persistNew(incoming)));
    }

    private KnowledgeDocument persistNew(KnowledgeDocumentJpaEntity incoming) {
        return documentPersistenceMapper.toDomain(documentJpaRepository.saveAndFlush(incoming));
    }

    private KnowledgeDocument replaceExisting(
            KnowledgeDocumentJpaEntity existing, KnowledgeDocumentJpaEntity incoming) {
        existing.overwriteScalarsFrom(incoming);
        existing.getChunks().clear();
        documentJpaRepository.flush();
        existing.getChunks().addAll(incoming.getChunks());
        return documentPersistenceMapper.toDomain(documentJpaRepository.saveAndFlush(existing));
    }

    @Override
    public Optional<KnowledgeDocument> findById(UUID id) {
        return documentJpaRepository.findById(id).map(documentPersistenceMapper::toDomain);
    }

    @Override
    public List<KnowledgeDocument> findAll() {
        return documentJpaRepository.findAll().stream()
                .map(documentPersistenceMapper::toDomain)
                .toList();
    }
}
