package com.superfercho.knowledge.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.knowledge.application.dto.CreateDocumentCommand;
import com.superfercho.knowledge.application.dto.DeactivateDocumentCommand;
import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.dto.GetDocumentCommand;
import com.superfercho.knowledge.application.dto.ListDocumentsCommand;
import com.superfercho.knowledge.application.dto.ProcessDocumentCommand;
import com.superfercho.knowledge.application.dto.ReactivateDocumentCommand;
import com.superfercho.knowledge.application.dto.ReplaceDocumentContentCommand;
import com.superfercho.knowledge.application.exception.DocumentNotFoundException;
import com.superfercho.knowledge.application.fakes.FakeDocumentChunker;
import com.superfercho.knowledge.application.fakes.FakeEmbeddingPort;
import com.superfercho.knowledge.application.fakes.FakeKnowledgeVectorStore;
import com.superfercho.knowledge.application.fakes.FixedClockPort;
import com.superfercho.knowledge.application.fakes.InMemoryKnowledgeDocumentRepository;
import com.superfercho.knowledge.domain.exception.InvalidDocumentException;
import com.superfercho.knowledge.domain.model.DocumentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-09-16T15:00:00Z");

    private InMemoryKnowledgeDocumentRepository documents;
    private FakeKnowledgeVectorStore vectorStore;
    private FakeDocumentChunker chunker;
    private FakeEmbeddingPort embeddings;
    private FixedClockPort clock;
    private CreateDocumentUseCase createDocument;
    private GetDocumentUseCase getDocument;
    private ListDocumentsUseCase listDocuments;
    private ReplaceDocumentContentUseCase replaceContent;
    private ProcessDocumentUseCase processDocument;
    private DeactivateDocumentUseCase deactivateDocument;
    private ReactivateDocumentUseCase reactivateDocument;

    @BeforeEach
    void setUp() {
        documents = new InMemoryKnowledgeDocumentRepository();
        vectorStore = new FakeKnowledgeVectorStore();
        chunker = new FakeDocumentChunker();
        embeddings = new FakeEmbeddingPort();
        clock = new FixedClockPort(NOW);
        createDocument = new CreateDocumentUseCase(documents, clock);
        getDocument = new GetDocumentUseCase(documents);
        listDocuments = new ListDocumentsUseCase(documents);
        replaceContent = new ReplaceDocumentContentUseCase(documents, vectorStore, clock);
        processDocument =
                new ProcessDocumentUseCase(documents, chunker, embeddings, vectorStore, clock);
        deactivateDocument = new DeactivateDocumentUseCase(documents, clock);
        reactivateDocument = new ReactivateDocumentUseCase(documents, clock);
    }

    @Test
    void shouldCreateReceivedDocumentUsingClock() {
        DocumentResult result = createDocument.execute(createCommand());

        assertEquals("Guía de frutas", result.title());
        assertEquals("manual-interno", result.source());
        assertEquals("Las frutas deben estar frescas.", result.content());
        assertEquals(DocumentStatus.RECEIVED, result.status());
        assertTrue(result.chunks().isEmpty());
        assertEquals(NOW, result.createdAt());
        assertEquals(NOW, result.updatedAt());
        assertEquals(1, documents.saved().size());
        assertEquals(result.id(), documents.saved().get(0).id().value());
        assertTrue(chunker.calls().isEmpty());
        assertTrue(embeddings.calls().isEmpty());
        assertTrue(vectorStore.upserts().isEmpty());
    }

    @Test
    void shouldGetExistingDocument() {
        DocumentResult created = createDocument.execute(createCommand());

        DocumentResult loaded = getDocument.execute(new GetDocumentCommand(created.id()));

        assertEquals(created, loaded);
    }

    @Test
    void shouldRejectGetWhenDocumentIsMissing() {
        UUID missing = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        DocumentNotFoundException exception =
                assertThrows(
                        DocumentNotFoundException.class, () -> getDocument.execute(new GetDocumentCommand(missing)));

        assertEquals("Document not found: " + missing, exception.getMessage());
    }

    @Test
    void shouldListAllDocuments() {
        DocumentResult first = createDocument.execute(createCommand());
        DocumentResult second =
                createDocument.execute(new CreateDocumentCommand("Otra guía", "faq", "Contenido extra."));

        List<DocumentResult> listed = listDocuments.execute(new ListDocumentsCommand());

        assertEquals(List.of(first, second), listed);
        assertEquals(2, documents.findAll().size());
    }

    @Test
    void shouldReplaceContentDeleteVectorsAndStayReceived() {
        DocumentResult created = createDocument.execute(createCommand());
        chunker.succeedWith("Las frutas deben estar frescas.");
        processDocument.execute(new ProcessDocumentCommand(created.id()));
        assertEquals(1, vectorStore.embeddingsOf(created.id()).size());

        DocumentResult replaced = replaceContent.execute(
                new ReplaceDocumentContentCommand(created.id(), "Texto nuevo para indexar."));

        assertEquals(DocumentStatus.RECEIVED, replaced.status());
        assertEquals("Texto nuevo para indexar.", replaced.content());
        assertTrue(replaced.chunks().isEmpty());
        assertEquals(List.of(created.id()), vectorStore.deletes());
        assertTrue(vectorStore.embeddingsOf(created.id()).isEmpty());
        assertEquals(1, embeddings.calls().size());
        assertEquals(DocumentStatus.RECEIVED, documents.findById(created.id()).orElseThrow().status());
    }

    @Test
    void shouldDeactivateReadyDocumentWithoutDeletingVectors() {
        DocumentResult created = createDocument.execute(createCommand());
        chunker.succeedWith("Las frutas deben estar frescas.");
        processDocument.execute(new ProcessDocumentCommand(created.id()));

        DocumentResult deactivated = deactivateDocument.execute(new DeactivateDocumentCommand(created.id()));

        assertEquals(DocumentStatus.INACTIVE, deactivated.status());
        assertEquals(1, deactivated.chunks().size());
        assertTrue(vectorStore.deletes().isEmpty());
        assertEquals(1, vectorStore.embeddingsOf(created.id()).size());
    }

    @Test
    void shouldReactivateInactiveDocumentWithoutReembedding() {
        DocumentResult created = createDocument.execute(createCommand());
        chunker.succeedWith("Las frutas deben estar frescas.");
        processDocument.execute(new ProcessDocumentCommand(created.id()));
        deactivateDocument.execute(new DeactivateDocumentCommand(created.id()));
        int embeddingCalls = embeddings.calls().size();

        DocumentResult reactivated = reactivateDocument.execute(new ReactivateDocumentCommand(created.id()));

        assertEquals(DocumentStatus.READY, reactivated.status());
        assertEquals(embeddingCalls, embeddings.calls().size());
        assertTrue(vectorStore.deletes().isEmpty());
    }

    @Test
    void shouldRejectDeactivateWhenDocumentIsMissing() {
        UUID missing = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        assertThrows(
                DocumentNotFoundException.class,
                () -> deactivateDocument.execute(new DeactivateDocumentCommand(missing)));
    }

    @Test
    void shouldRejectReplaceWhenDocumentIsMissing() {
        UUID missing = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

        assertThrows(
                DocumentNotFoundException.class,
                () -> replaceContent.execute(new ReplaceDocumentContentCommand(missing, "nuevo")));
    }

    @Test
    void shouldRejectCreateWhenTitleIsBlank() {
        assertThrows(
                InvalidDocumentException.class,
                () -> createDocument.execute(new CreateDocumentCommand("  ", "manual", "texto")));
    }

    private static CreateDocumentCommand createCommand() {
        return new CreateDocumentCommand("Guía de frutas", "manual-interno", "Las frutas deben estar frescas.");
    }
}
