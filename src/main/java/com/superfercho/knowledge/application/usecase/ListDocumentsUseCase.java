package com.superfercho.knowledge.application.usecase;

import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.dto.ListDocumentsCommand;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import java.util.List;

public final class ListDocumentsUseCase {

    private final KnowledgeDocumentRepository documentRepository;

    public ListDocumentsUseCase(KnowledgeDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public List<DocumentResult> execute(ListDocumentsCommand command) {
        return documentRepository.findAll().stream().map(DocumentResult::from).toList();
    }
}
