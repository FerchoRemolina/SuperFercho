package com.superfercho.knowledge.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.knowledge.application.dto.ChunkResult;
import com.superfercho.knowledge.application.dto.CreateDocumentCommand;
import com.superfercho.knowledge.application.dto.DeactivateDocumentCommand;
import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.dto.GetDocumentCommand;
import com.superfercho.knowledge.application.dto.KnowledgeSearchHit;
import com.superfercho.knowledge.application.dto.KnowledgeSearchResult;
import com.superfercho.knowledge.application.dto.ListDocumentsCommand;
import com.superfercho.knowledge.application.dto.ProcessDocumentCommand;
import com.superfercho.knowledge.application.dto.ReactivateDocumentCommand;
import com.superfercho.knowledge.application.dto.ReplaceDocumentContentCommand;
import com.superfercho.knowledge.application.dto.SearchKnowledgeCommand;
import com.superfercho.knowledge.application.exception.DocumentNotFoundException;
import com.superfercho.knowledge.application.exception.InvalidSearchRequestException;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.application.usecase.CreateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.DeactivateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.GetDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ListDocumentsUseCase;
import com.superfercho.knowledge.application.usecase.ProcessDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ReactivateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ReplaceDocumentContentUseCase;
import com.superfercho.knowledge.application.usecase.SearchKnowledgeUseCase;
import com.superfercho.knowledge.domain.exception.InvalidDocumentException;
import com.superfercho.knowledge.domain.model.DocumentStatus;
import com.superfercho.platform.error.ApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = KnowledgeDocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({KnowledgeExceptionHandler.class, ApiExceptionHandler.class})
class KnowledgeDocumentControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:30:00Z");
    private static final UUID DOCUMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CHUNK_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateDocumentUseCase createDocumentUseCase;

    @MockitoBean
    private GetDocumentUseCase getDocumentUseCase;

    @MockitoBean
    private ListDocumentsUseCase listDocumentsUseCase;

    @MockitoBean
    private ReplaceDocumentContentUseCase replaceDocumentContentUseCase;

    @MockitoBean
    private ProcessDocumentUseCase processDocumentUseCase;

    @MockitoBean
    private DeactivateDocumentUseCase deactivateDocumentUseCase;

    @MockitoBean
    private ReactivateDocumentUseCase reactivateDocumentUseCase;

    @MockitoBean
    private SearchKnowledgeUseCase searchKnowledgeUseCase;

    @Test
    void shouldCreateDocument() throws Exception {
        when(createDocumentUseCase.execute(any())).thenReturn(documentResult(DocumentStatus.RECEIVED, List.of()));

        mockMvc.perform(post("/api/v1/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDocumentJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.endsWith("/api/v1/knowledge/documents/" + DOCUMENT_ID)))
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$.title").value("Pollo asado"))
                .andExpect(jsonPath("$.source").value("manual"))
                .andExpect(jsonPath("$.content").value("El pollo asado se cocina a 180 grados."))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.chunks").isEmpty())
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));

        verify(createDocumentUseCase)
                .execute(new CreateDocumentCommand(
                        "Pollo asado", "manual", "El pollo asado se cocina a 180 grados."));
        verifyNoInteractions(
                getDocumentUseCase,
                listDocumentsUseCase,
                replaceDocumentContentUseCase,
                processDocumentUseCase,
                deactivateDocumentUseCase,
                reactivateDocumentUseCase,
                searchKnowledgeUseCase);
    }

    @Test
    void shouldGetDocumentById() throws Exception {
        when(getDocumentUseCase.execute(new GetDocumentCommand(DOCUMENT_ID)))
                .thenReturn(documentResult(DocumentStatus.READY, List.of(chunkResult())));

        mockMvc.perform(get("/api/v1/knowledge/documents/{documentId}", DOCUMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$.title").value("Pollo asado"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.chunks[0].id").value(CHUNK_ID.toString()))
                .andExpect(jsonPath("$.chunks[0].position").value(0))
                .andExpect(jsonPath("$.chunks[0].text").value("El pollo asado se cocina a 180 grados."))
                .andExpect(jsonPath("$.chunks[0].embedded").value(true));

        verify(getDocumentUseCase).execute(new GetDocumentCommand(DOCUMENT_ID));
        verifyNoInteractions(
                createDocumentUseCase,
                listDocumentsUseCase,
                replaceDocumentContentUseCase,
                processDocumentUseCase,
                deactivateDocumentUseCase,
                reactivateDocumentUseCase,
                searchKnowledgeUseCase);
    }

    @Test
    void shouldListDocuments() throws Exception {
        when(listDocumentsUseCase.execute(new ListDocumentsCommand()))
                .thenReturn(List.of(documentResult(DocumentStatus.RECEIVED, List.of())));

        mockMvc.perform(get("/api/v1/knowledge/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$[0].title").value("Pollo asado"))
                .andExpect(jsonPath("$[0].status").value("RECEIVED"));

        verify(listDocumentsUseCase).execute(new ListDocumentsCommand());
        verifyNoInteractions(
                createDocumentUseCase,
                getDocumentUseCase,
                replaceDocumentContentUseCase,
                processDocumentUseCase,
                deactivateDocumentUseCase,
                reactivateDocumentUseCase,
                searchKnowledgeUseCase);
    }

    @Test
    void shouldReplaceDocumentContent() throws Exception {
        when(replaceDocumentContentUseCase.execute(any()))
                .thenReturn(documentResult(DocumentStatus.RECEIVED, List.of()));

        mockMvc.perform(put("/api/v1/knowledge/documents/{documentId}/content", DOCUMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Contenido actualizado del pollo asado."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        verify(replaceDocumentContentUseCase)
                .execute(new ReplaceDocumentContentCommand(DOCUMENT_ID, "Contenido actualizado del pollo asado."));
        verifyNoInteractions(
                createDocumentUseCase,
                getDocumentUseCase,
                listDocumentsUseCase,
                processDocumentUseCase,
                deactivateDocumentUseCase,
                reactivateDocumentUseCase,
                searchKnowledgeUseCase);
    }

    @Test
    void shouldProcessDocument() throws Exception {
        when(processDocumentUseCase.execute(new ProcessDocumentCommand(DOCUMENT_ID)))
                .thenReturn(documentResult(DocumentStatus.READY, List.of(chunkResult())));

        mockMvc.perform(post("/api/v1/knowledge/documents/{documentId}/process", DOCUMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.chunks[0].id").value(CHUNK_ID.toString()))
                .andExpect(jsonPath("$.chunks[0].embedded").value(true));

        verify(processDocumentUseCase).execute(new ProcessDocumentCommand(DOCUMENT_ID));
        verifyNoInteractions(
                createDocumentUseCase,
                getDocumentUseCase,
                listDocumentsUseCase,
                replaceDocumentContentUseCase,
                deactivateDocumentUseCase,
                reactivateDocumentUseCase,
                searchKnowledgeUseCase);
    }

    @Test
    void shouldDeactivateDocument() throws Exception {
        when(deactivateDocumentUseCase.execute(new DeactivateDocumentCommand(DOCUMENT_ID)))
                .thenReturn(documentResult(DocumentStatus.INACTIVE, List.of(chunkResult())));

        mockMvc.perform(post("/api/v1/knowledge/documents/{documentId}/deactivate", DOCUMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(deactivateDocumentUseCase).execute(new DeactivateDocumentCommand(DOCUMENT_ID));
        verifyNoInteractions(
                createDocumentUseCase,
                getDocumentUseCase,
                listDocumentsUseCase,
                replaceDocumentContentUseCase,
                processDocumentUseCase,
                reactivateDocumentUseCase,
                searchKnowledgeUseCase);
    }

    @Test
    void shouldReactivateDocument() throws Exception {
        when(reactivateDocumentUseCase.execute(new ReactivateDocumentCommand(DOCUMENT_ID)))
                .thenReturn(documentResult(DocumentStatus.READY, List.of(chunkResult())));

        mockMvc.perform(post("/api/v1/knowledge/documents/{documentId}/reactivate", DOCUMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("READY"));

        verify(reactivateDocumentUseCase).execute(new ReactivateDocumentCommand(DOCUMENT_ID));
        verifyNoInteractions(
                createDocumentUseCase,
                getDocumentUseCase,
                listDocumentsUseCase,
                replaceDocumentContentUseCase,
                processDocumentUseCase,
                deactivateDocumentUseCase,
                searchKnowledgeUseCase);
    }

    @Test
    void shouldSearchKnowledge() throws Exception {
        when(searchKnowledgeUseCase.execute(new SearchKnowledgeCommand("pollo", 5)))
                .thenReturn(new KnowledgeSearchResult(List.of(searchHit())));

        mockMvc.perform(get("/api/v1/knowledge/search").param("query", "pollo").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits[0].documentId").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$.hits[0].chunkId").value(CHUNK_ID.toString()))
                .andExpect(jsonPath("$.hits[0].title").value("Pollo asado"))
                .andExpect(jsonPath("$.hits[0].source").value("manual"))
                .andExpect(jsonPath("$.hits[0].chunkText").value("El pollo asado se cocina a 180 grados."))
                .andExpect(jsonPath("$.hits[0].score").value(0.12));

        verify(searchKnowledgeUseCase).execute(new SearchKnowledgeCommand("pollo", 5));
        verifyNoInteractions(
                createDocumentUseCase,
                getDocumentUseCase,
                listDocumentsUseCase,
                replaceDocumentContentUseCase,
                processDocumentUseCase,
                deactivateDocumentUseCase,
                reactivateDocumentUseCase);
    }

    @Test
    void shouldMapDocumentNotFoundToNotFound() throws Exception {
        when(getDocumentUseCase.execute(any())).thenThrow(new DocumentNotFoundException(DOCUMENT_ID));

        mockMvc.perform(get("/api/v1/knowledge/documents/{documentId}", DOCUMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Document not found: " + DOCUMENT_ID))
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void shouldMapInvalidDocumentToBadRequest() throws Exception {
        when(createDocumentUseCase.execute(any()))
                .thenThrow(new InvalidDocumentException("title cannot be null or blank"));

        mockMvc.perform(post("/api/v1/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDocumentJson()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("title cannot be null or blank"))
                .andExpect(jsonPath("$.code").value("INVALID_DOCUMENT"));
    }

    @Test
    void shouldMapProcessingFailureToConflict() throws Exception {
        when(processDocumentUseCase.execute(any()))
                .thenThrow(new KnowledgeProcessingException("document processing failed"));

        mockMvc.perform(post("/api/v1/knowledge/documents/{documentId}/process", DOCUMENT_ID))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("document processing failed"))
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_PROCESSING_FAILED"));
    }

    @Test
    void shouldMapInvalidSearchLimitToBadRequest() throws Exception {
        when(searchKnowledgeUseCase.execute(any()))
                .thenThrow(new InvalidSearchRequestException("limit must be between 1 and 20"));

        mockMvc.perform(get("/api/v1/knowledge/search").param("query", "pollo").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("limit must be between 1 and 20"))
                .andExpect(jsonPath("$.code").value("INVALID_SEARCH_REQUEST"));

        verify(searchKnowledgeUseCase).execute(new SearchKnowledgeCommand("pollo", 0));
    }

    @Test
    void shouldRejectMalformedCreateBody() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(
                createDocumentUseCase,
                getDocumentUseCase,
                listDocumentsUseCase,
                replaceDocumentContentUseCase,
                processDocumentUseCase,
                deactivateDocumentUseCase,
                reactivateDocumentUseCase,
                searchKnowledgeUseCase);
    }

    @Test
    void shouldRejectInvalidDocumentId() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/documents/{documentId}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getDocumentUseCase);
    }

    private static String createDocumentJson() {
        return """
                {
                  "title": "Pollo asado",
                  "source": "manual",
                  "content": "El pollo asado se cocina a 180 grados."
                }
                """;
    }

    private static DocumentResult documentResult(DocumentStatus status, List<ChunkResult> chunks) {
        return new DocumentResult(
                DOCUMENT_ID,
                "Pollo asado",
                "manual",
                "El pollo asado se cocina a 180 grados.",
                status,
                chunks,
                CREATED_AT,
                UPDATED_AT);
    }

    private static ChunkResult chunkResult() {
        return new ChunkResult(CHUNK_ID, 0, "El pollo asado se cocina a 180 grados.", true);
    }

    private static KnowledgeSearchHit searchHit() {
        return new KnowledgeSearchHit(
                DOCUMENT_ID,
                CHUNK_ID,
                "Pollo asado",
                "manual",
                "El pollo asado se cocina a 180 grados.",
                0.12);
    }
}
