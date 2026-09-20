package com.superfercho.knowledge.application.usecase;

import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.dto.ReplaceDocumentContentCommand;
import com.superfercho.knowledge.application.exception.DocumentNotFoundException;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.application.port.ClockPort;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.application.port.KnowledgeVectorStorePort;
import com.superfercho.knowledge.domain.model.DocumentContent;
import com.superfercho.knowledge.domain.model.DocumentId;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;

public final class ReplaceDocumentContentUseCase {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeVectorStorePort vectorStore;
    private final ClockPort clockPort;

    public ReplaceDocumentContentUseCase(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeVectorStorePort vectorStore,
            ClockPort clockPort) {
        this.documentRepository = documentRepository;
        this.vectorStore = vectorStore;
        this.clockPort = clockPort;
    }

    public DocumentResult execute(ReplaceDocumentContentCommand command) {
        DocumentId documentId = new DocumentId(command.documentId());
        KnowledgeDocument document = documentRepository
                .findById(documentId.value())
                .orElseThrow(() -> new DocumentNotFoundException(documentId.value()));
        KnowledgeDocument replaced =
                document.replaceContent(new DocumentContent(command.content()), clockPort.now());
        try {
            vectorStore.deleteByDocumentId(documentId.value());
        } catch (KnowledgeProcessingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new KnowledgeProcessingException("failed to delete document embeddings");
        }
        return DocumentResult.from(documentRepository.save(replaced));
    }
}
