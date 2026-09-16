package com.superfercho.knowledge.application.usecase;

import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.dto.ReactivateDocumentCommand;
import com.superfercho.knowledge.application.exception.DocumentNotFoundException;
import com.superfercho.knowledge.application.port.ClockPort;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.domain.model.DocumentId;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;

public final class ReactivateDocumentUseCase {

    private final KnowledgeDocumentRepository documentRepository;
    private final ClockPort clockPort;

    public ReactivateDocumentUseCase(KnowledgeDocumentRepository documentRepository, ClockPort clockPort) {
        this.documentRepository = documentRepository;
        this.clockPort = clockPort;
    }

    public DocumentResult execute(ReactivateDocumentCommand command) {
        DocumentId documentId = new DocumentId(command.documentId());
        KnowledgeDocument document = documentRepository
                .findById(documentId.value())
                .orElseThrow(() -> new DocumentNotFoundException(documentId.value()));
        return DocumentResult.from(documentRepository.save(document.reactivate(clockPort.now())));
    }
}
