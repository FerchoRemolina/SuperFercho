package com.superfercho.knowledge.application.usecase;

import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.dto.GetDocumentCommand;
import com.superfercho.knowledge.application.exception.DocumentNotFoundException;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.domain.model.DocumentId;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;

public final class GetDocumentUseCase {

    private final KnowledgeDocumentRepository documentRepository;

    public GetDocumentUseCase(KnowledgeDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public DocumentResult execute(GetDocumentCommand command) {
        DocumentId documentId = new DocumentId(command.documentId());
        KnowledgeDocument document = documentRepository
                .findById(documentId.value())
                .orElseThrow(() -> new DocumentNotFoundException(documentId.value()));
        return DocumentResult.from(document);
    }
}
