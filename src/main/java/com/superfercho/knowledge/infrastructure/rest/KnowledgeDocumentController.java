package com.superfercho.knowledge.infrastructure.rest;

import com.superfercho.knowledge.application.dto.CreateDocumentCommand;
import com.superfercho.knowledge.application.dto.DeactivateDocumentCommand;
import com.superfercho.knowledge.application.dto.GetDocumentCommand;
import com.superfercho.knowledge.application.dto.ListDocumentsCommand;
import com.superfercho.knowledge.application.dto.ProcessDocumentCommand;
import com.superfercho.knowledge.application.dto.ReactivateDocumentCommand;
import com.superfercho.knowledge.application.dto.ReplaceDocumentContentCommand;
import com.superfercho.knowledge.application.dto.SearchKnowledgeCommand;
import com.superfercho.knowledge.application.usecase.CreateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.DeactivateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.GetDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ListDocumentsUseCase;
import com.superfercho.knowledge.application.usecase.ProcessDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ReactivateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ReplaceDocumentContentUseCase;
import com.superfercho.knowledge.application.usecase.SearchKnowledgeUseCase;
import com.superfercho.knowledge.infrastructure.rest.dto.CreateDocumentRequest;
import com.superfercho.knowledge.infrastructure.rest.dto.DocumentRestResponse;
import com.superfercho.knowledge.infrastructure.rest.dto.KnowledgeSearchRestResponse;
import com.superfercho.knowledge.infrastructure.rest.dto.ReplaceDocumentContentRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/knowledge")
public class KnowledgeDocumentController {

    private final CreateDocumentUseCase createDocumentUseCase;
    private final GetDocumentUseCase getDocumentUseCase;
    private final ListDocumentsUseCase listDocumentsUseCase;
    private final ReplaceDocumentContentUseCase replaceDocumentContentUseCase;
    private final ProcessDocumentUseCase processDocumentUseCase;
    private final DeactivateDocumentUseCase deactivateDocumentUseCase;
    private final ReactivateDocumentUseCase reactivateDocumentUseCase;
    private final SearchKnowledgeUseCase searchKnowledgeUseCase;

    public KnowledgeDocumentController(
            CreateDocumentUseCase createDocumentUseCase,
            GetDocumentUseCase getDocumentUseCase,
            ListDocumentsUseCase listDocumentsUseCase,
            ReplaceDocumentContentUseCase replaceDocumentContentUseCase,
            ProcessDocumentUseCase processDocumentUseCase,
            DeactivateDocumentUseCase deactivateDocumentUseCase,
            ReactivateDocumentUseCase reactivateDocumentUseCase,
            SearchKnowledgeUseCase searchKnowledgeUseCase) {
        this.createDocumentUseCase = createDocumentUseCase;
        this.getDocumentUseCase = getDocumentUseCase;
        this.listDocumentsUseCase = listDocumentsUseCase;
        this.replaceDocumentContentUseCase = replaceDocumentContentUseCase;
        this.processDocumentUseCase = processDocumentUseCase;
        this.deactivateDocumentUseCase = deactivateDocumentUseCase;
        this.reactivateDocumentUseCase = reactivateDocumentUseCase;
        this.searchKnowledgeUseCase = searchKnowledgeUseCase;
    }

    @PostMapping("/documents")
    public ResponseEntity<DocumentRestResponse> create(@RequestBody CreateDocumentRequest request) {
        DocumentRestResponse body = DocumentRestResponse.from(createDocumentUseCase.execute(
                new CreateDocumentCommand(request.title(), request.source(), request.content())));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(body.id())
                        .toUri())
                .body(body);
    }

    @GetMapping("/documents")
    public List<DocumentRestResponse> list() {
        return listDocumentsUseCase.execute(new ListDocumentsCommand()).stream()
                .map(DocumentRestResponse::from)
                .toList();
    }

    @GetMapping("/documents/{documentId}")
    public DocumentRestResponse get(@PathVariable UUID documentId) {
        return DocumentRestResponse.from(getDocumentUseCase.execute(new GetDocumentCommand(documentId)));
    }

    @PutMapping("/documents/{documentId}/content")
    public DocumentRestResponse replaceContent(
            @PathVariable UUID documentId, @RequestBody ReplaceDocumentContentRequest request) {
        return DocumentRestResponse.from(replaceDocumentContentUseCase.execute(
                new ReplaceDocumentContentCommand(documentId, request.content())));
    }

    @PostMapping("/documents/{documentId}/process")
    public DocumentRestResponse process(@PathVariable UUID documentId) {
        return DocumentRestResponse.from(processDocumentUseCase.execute(new ProcessDocumentCommand(documentId)));
    }

    @PostMapping("/documents/{documentId}/deactivate")
    public DocumentRestResponse deactivate(@PathVariable UUID documentId) {
        return DocumentRestResponse.from(
                deactivateDocumentUseCase.execute(new DeactivateDocumentCommand(documentId)));
    }

    @PostMapping("/documents/{documentId}/reactivate")
    public DocumentRestResponse reactivate(@PathVariable UUID documentId) {
        return DocumentRestResponse.from(
                reactivateDocumentUseCase.execute(new ReactivateDocumentCommand(documentId)));
    }

    @GetMapping("/search")
    public KnowledgeSearchRestResponse search(
            @RequestParam(required = false) String query, @RequestParam int limit) {
        return KnowledgeSearchRestResponse.from(
                searchKnowledgeUseCase.execute(new SearchKnowledgeCommand(query, limit)));
    }
}
