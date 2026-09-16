package com.superfercho.knowledge.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.knowledge.application.dto.ChunkEmbedding;
import com.superfercho.knowledge.application.dto.CreateDocumentCommand;
import com.superfercho.knowledge.application.dto.DeactivateDocumentCommand;
import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.dto.ProcessDocumentCommand;
import com.superfercho.knowledge.application.exception.DocumentNotFoundException;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
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

class ProcessDocumentUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-16T16:00:00Z");

    private InMemoryKnowledgeDocumentRepository documents;
    private FakeDocumentChunker chunker;
    private FakeEmbeddingPort embeddings;
    private FakeKnowledgeVectorStore vectorStore;
    private ProcessDocumentUseCase processDocument;
    private CreateDocumentUseCase createDocument;
    private DeactivateDocumentUseCase deactivateDocument;

    @BeforeEach
    void setUp() {
        documents = new InMemoryKnowledgeDocumentRepository();
        chunker = new FakeDocumentChunker();
        embeddings = new FakeEmbeddingPort();
        vectorStore = new FakeKnowledgeVectorStore();
        FixedClockPort clock = new FixedClockPort(NOW);
        processDocument = new ProcessDocumentUseCase(documents, chunker, embeddings, vectorStore, clock);
        createDocument = new CreateDocumentUseCase(documents, clock);
        deactivateDocument = new DeactivateDocumentUseCase(documents, clock);
    }

    @Test
    void shouldProcessReceivedDocumentUntilReady() {
        DocumentResult created = create();
        chunker.succeedWith("uno", "dos");

        DocumentResult processed = processDocument.execute(new ProcessDocumentCommand(created.id()));

        assertEquals(DocumentStatus.READY, processed.status());
        assertEquals(2, processed.chunks().size());
        assertTrue(processed.chunks().get(0).embedded());
        assertTrue(processed.chunks().get(1).embedded());
        assertEquals(1, chunker.calls().size());
        assertEquals(List.of("uno"), embeddings.calls().get(0));
        assertEquals(List.of("dos"), embeddings.calls().get(1));
        assertEquals(2, vectorStore.upserts().size());
        ChunkEmbedding first = vectorStore.upserts().get(0);
        assertEquals(created.id(), first.documentId());
        assertEquals(processed.chunks().get(0).id(), first.chunkId());
        assertEquals(0, first.position());
        assertEquals(processed.chunks().get(1).id(), vectorStore.upserts().get(1).chunkId());
    }

    @Test
    void shouldResumeChunkedDocumentWithoutRechunking() {
        DocumentResult created = create();
        chunker.succeedWith("uno", "dos");
        embeddings.failOnCall(2, new KnowledgeProcessingException("embedding failed"));
        assertThrows(
                KnowledgeProcessingException.class,
                () -> processDocument.execute(new ProcessDocumentCommand(created.id())));
        assertEquals(DocumentStatus.CHUNKED, documents.findById(created.id()).orElseThrow().status());
        int chunkCalls = chunker.calls().size();
        embeddings.succeed();

        DocumentResult processed = processDocument.execute(new ProcessDocumentCommand(created.id()));

        assertEquals(DocumentStatus.READY, processed.status());
        assertEquals(chunkCalls, chunker.calls().size());
        assertEquals(3, embeddings.calls().size());
        assertEquals(List.of("dos"), embeddings.calls().get(2));
        assertEquals(2, vectorStore.upserts().size());
        assertTrue(processed.chunks().get(0).embedded());
        assertTrue(processed.chunks().get(1).embedded());
    }

    @Test
    void shouldReprocessFailedDocumentFromScratch() {
        DocumentResult created = create();
        chunker.failWith(new KnowledgeProcessingException("chunker failed"));
        assertThrows(
                KnowledgeProcessingException.class,
                () -> processDocument.execute(new ProcessDocumentCommand(created.id())));
        assertEquals(DocumentStatus.FAILED, documents.findById(created.id()).orElseThrow().status());
        chunker.succeedWith("nuevo");

        DocumentResult processed = processDocument.execute(new ProcessDocumentCommand(created.id()));

        assertEquals(DocumentStatus.READY, processed.status());
        assertEquals(1, processed.chunks().size());
        assertEquals("nuevo", processed.chunks().get(0).text());
        assertEquals(List.of(created.id()), vectorStore.deletes());
        assertEquals(1, vectorStore.upserts().size());
        assertEquals(2, chunker.calls().size());
    }

    @Test
    void shouldRejectProcessWhenReady() {
        DocumentResult created = create();
        chunker.succeedWith("uno");
        processDocument.execute(new ProcessDocumentCommand(created.id()));

        assertThrows(
                InvalidDocumentException.class,
                () -> processDocument.execute(new ProcessDocumentCommand(created.id())));
    }

    @Test
    void shouldRejectProcessWhenInactive() {
        DocumentResult created = create();
        chunker.succeedWith("uno");
        processDocument.execute(new ProcessDocumentCommand(created.id()));
        deactivateDocument.execute(new DeactivateDocumentCommand(created.id()));

        assertThrows(
                InvalidDocumentException.class,
                () -> processDocument.execute(new ProcessDocumentCommand(created.id())));
    }

    @Test
    void shouldMarkFailedWhenChunkerFails() {
        DocumentResult created = create();
        chunker.failWith(new IllegalStateException("tokenizer exploded"));

        KnowledgeProcessingException exception =
                assertThrows(
                        KnowledgeProcessingException.class,
                        () -> processDocument.execute(new ProcessDocumentCommand(created.id())));

        assertEquals("failed to chunk document", exception.getMessage());
        assertEquals(DocumentStatus.FAILED, documents.findById(created.id()).orElseThrow().status());
        assertTrue(vectorStore.upserts().isEmpty());
    }

    @Test
    void shouldKeepChunkedProgressWhenEmbeddingFails() {
        DocumentResult created = create();
        chunker.succeedWith("uno", "dos");
        embeddings.failOnCall(1, new KnowledgeProcessingException("embedding failed"));

        assertThrows(
                KnowledgeProcessingException.class,
                () -> processDocument.execute(new ProcessDocumentCommand(created.id())));

        DocumentResult stored = DocumentResult.from(documents.findById(created.id()).orElseThrow());
        assertEquals(DocumentStatus.CHUNKED, stored.status());
        assertFalse(stored.chunks().get(0).embedded());
        assertFalse(stored.chunks().get(1).embedded());
        assertTrue(vectorStore.upserts().isEmpty());
    }

    @Test
    void shouldKeepPreviousUpsertWhenVectorStoreFails() {
        DocumentResult created = create();
        chunker.succeedWith("uno", "dos");
        vectorStore.failOnUpsert(2, new KnowledgeProcessingException("vector store failed"));

        assertThrows(
                KnowledgeProcessingException.class,
                () -> processDocument.execute(new ProcessDocumentCommand(created.id())));

        DocumentResult stored = DocumentResult.from(documents.findById(created.id()).orElseThrow());
        assertEquals(DocumentStatus.CHUNKED, stored.status());
        assertTrue(stored.chunks().get(0).embedded());
        assertFalse(stored.chunks().get(1).embedded());
        assertEquals(1, vectorStore.upserts().size());
        assertEquals(stored.chunks().get(0).id(), vectorStore.upserts().get(0).chunkId());
    }

    @Test
    void shouldRejectProcessWhenDocumentIsMissing() {
        UUID missing = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        assertThrows(
                DocumentNotFoundException.class,
                () -> processDocument.execute(new ProcessDocumentCommand(missing)));
    }

    @Test
    void shouldMarkFailedWhenChunkerReturnsNoChunks() {
        DocumentResult created = create();
        chunker.succeedWith();

        KnowledgeProcessingException exception =
                assertThrows(
                        KnowledgeProcessingException.class,
                        () -> processDocument.execute(new ProcessDocumentCommand(created.id())));

        assertEquals("chunker produced no chunks", exception.getMessage());
        assertEquals(DocumentStatus.FAILED, documents.findById(created.id()).orElseThrow().status());
    }

    private DocumentResult create() {
        return createDocument.execute(
                new CreateDocumentCommand("Guía de frutas", "manual-interno", "Las frutas deben estar frescas."));
    }
}
