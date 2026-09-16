package com.superfercho.knowledge.application.usecase;

import com.superfercho.knowledge.application.dto.CreateDocumentCommand;
import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.port.ClockPort;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.domain.model.DocumentContent;
import com.superfercho.knowledge.domain.model.DocumentSource;
import com.superfercho.knowledge.domain.model.DocumentTitle;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;

public final class CreateDocumentUseCase {

    private final KnowledgeDocumentRepository documentRepository;
    private final ClockPort clockPort;

    public CreateDocumentUseCase(KnowledgeDocumentRepository documentRepository, ClockPort clockPort) {
        this.documentRepository = documentRepository;
        this.clockPort = clockPort;
    }

    public DocumentResult execute(CreateDocumentCommand command) {
        KnowledgeDocument document = KnowledgeDocument.create(
                new DocumentTitle(command.title()),
                new DocumentSource(command.source()),
                new DocumentContent(command.content()),
                clockPort.now());
        return DocumentResult.from(documentRepository.save(document));
    }
}
