package com.superfercho.knowledge.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.knowledge.domain.exception.InvalidDocumentException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class KnowledgeDocumentTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-16T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-09-16T10:05:00Z");
    private static final DocumentTitle TITLE = new DocumentTitle("Guía de frutas");
    private static final DocumentSource SOURCE = new DocumentSource("manual-interno");
    private static final DocumentContent CONTENT = new DocumentContent("Las frutas deben estar frescas.");

    @Test
    void shouldCreateReceivedDocumentWithoutChunks() {
        KnowledgeDocument document = received();

        assertNotNull(document.id());
        assertEquals(TITLE, document.title());
        assertEquals(SOURCE, document.source());
        assertEquals(CONTENT, document.content());
        assertEquals(DocumentStatus.RECEIVED, document.status());
        assertTrue(document.chunks().isEmpty());
        assertEquals(CREATED_AT, document.createdAt());
        assertEquals(CREATED_AT, document.updatedAt());
        assertFalse(document.searchable());
    }

    @Test
    void shouldAssignDistinctDocumentIds() {
        KnowledgeDocument first = received();
        KnowledgeDocument second = received();

        assertNotEquals(first.id(), second.id());
    }

    @Test
    void shouldRejectCreateWhenTitleIsNull() {
        assertThrows(InvalidDocumentException.class, () -> KnowledgeDocument.create(null, SOURCE, CONTENT, CREATED_AT));
    }

    @Test
    void shouldRejectCreateWhenSourceIsNull() {
        assertThrows(InvalidDocumentException.class, () -> KnowledgeDocument.create(TITLE, null, CONTENT, CREATED_AT));
    }

    @Test
    void shouldRejectCreateWhenContentIsNull() {
        assertThrows(InvalidDocumentException.class, () -> KnowledgeDocument.create(TITLE, SOURCE, null, CREATED_AT));
    }

    @Test
    void shouldRejectCreateWhenCreatedAtIsNull() {
        assertThrows(InvalidDocumentException.class, () -> KnowledgeDocument.create(TITLE, SOURCE, CONTENT, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectBlankTitle(String blank) {
        assertThrows(InvalidDocumentException.class, () -> new DocumentTitle(blank));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectBlankSource(String blank) {
        assertThrows(InvalidDocumentException.class, () -> new DocumentSource(blank));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectBlankContent(String blank) {
        assertThrows(InvalidDocumentException.class, () -> new DocumentContent(blank));
    }

    @Test
    void shouldRejectNullDocumentId() {
        assertThrows(InvalidDocumentException.class, () -> new DocumentId(null));
    }

    @Test
    void shouldRejectNullChunkId() {
        assertThrows(InvalidDocumentException.class, () -> new ChunkId(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectBlankChunkText(String blank) {
        assertThrows(InvalidDocumentException.class, () -> new ChunkText(blank));
    }

    @Test
    void shouldRejectNegativeChunkPosition() {
        assertThrows(InvalidDocumentException.class, () -> new ChunkPosition(-1));
    }

    @Test
    void shouldReplaceChunksAndBecomeChunked() {
        KnowledgeDocument document = received().replaceChunks(List.of(text("uno"), text("dos")), LATER);

        assertEquals(DocumentStatus.CHUNKED, document.status());
        assertEquals(2, document.chunks().size());
        assertEquals(0, document.chunks().get(0).position().value());
        assertEquals(1, document.chunks().get(1).position().value());
        assertEquals("uno", document.chunks().get(0).text().value());
        assertEquals("dos", document.chunks().get(1).text().value());
        assertFalse(document.chunks().get(0).embedded());
        assertFalse(document.chunks().get(1).embedded());
        assertEquals(LATER, document.updatedAt());
        assertFalse(document.searchable());
    }

    @Test
    void shouldRejectEmptyChunkList() {
        assertThrows(InvalidDocumentException.class, () -> received().replaceChunks(List.of(), LATER));
    }

    @Test
    void shouldRejectNullChunkList() {
        assertThrows(InvalidDocumentException.class, () -> received().replaceChunks(null, LATER));
    }

    @Test
    void shouldRejectNullChunkTextInList() {
        List<ChunkText> texts = new ArrayList<>();
        texts.add(text("uno"));
        texts.add(null);
        assertThrows(InvalidDocumentException.class, () -> received().replaceChunks(texts, LATER));
    }

    @Test
    void shouldKeepChunkListIndependentFromCaller() {
        List<ChunkText> texts = new ArrayList<>();
        texts.add(text("uno"));
        KnowledgeDocument document = received().replaceChunks(texts, LATER);
        texts.add(text("dos"));

        assertEquals(1, document.chunks().size());
        assertThrows(UnsupportedOperationException.class, () -> document.chunks().add(document.chunks().get(0)));
    }

    @Test
    void shouldMarkOneChunkEmbeddedAndStayChunked() {
        KnowledgeDocument chunked = received().replaceChunks(List.of(text("uno"), text("dos")), LATER);

        KnowledgeDocument updated = chunked.markChunkEmbedded(chunked.chunks().get(0).id(), LATER);

        assertEquals(DocumentStatus.CHUNKED, updated.status());
        assertTrue(updated.chunks().get(0).embedded());
        assertFalse(updated.chunks().get(1).embedded());
        assertFalse(updated.searchable());
    }

    @Test
    void shouldBecomeReadyWhenEveryChunkIsEmbedded() {
        KnowledgeDocument chunked = received().replaceChunks(List.of(text("uno"), text("dos")), LATER);
        KnowledgeDocument first = chunked.markChunkEmbedded(chunked.chunks().get(0).id(), LATER);
        KnowledgeDocument ready = first.markChunkEmbedded(first.chunks().get(1).id(), LATER);

        assertEquals(DocumentStatus.READY, ready.status());
        assertTrue(ready.chunks().get(0).embedded());
        assertTrue(ready.chunks().get(1).embedded());
        assertTrue(ready.searchable());
    }

    @Test
    void shouldRejectUnknownChunkId() {
        KnowledgeDocument chunked = received().replaceChunks(List.of(text("uno")), LATER);

        assertThrows(
                InvalidDocumentException.class,
                () -> chunked.markChunkEmbedded(new ChunkId(UUID.randomUUID()), LATER));
    }

    @Test
    void shouldRejectMarkEmbeddedWhenReceived() {
        assertThrows(
                InvalidDocumentException.class,
                () -> received().markChunkEmbedded(new ChunkId(UUID.randomUUID()), LATER));
    }

    @Test
    void shouldFailFromReceived() {
        KnowledgeDocument failed = received().markFailed(LATER);

        assertEquals(DocumentStatus.FAILED, failed.status());
        assertTrue(failed.chunks().isEmpty());
        assertFalse(failed.searchable());
    }

    @Test
    void shouldFailFromChunked() {
        KnowledgeDocument chunked = received().replaceChunks(List.of(text("uno")), LATER);

        KnowledgeDocument failed = chunked.markFailed(LATER);

        assertEquals(DocumentStatus.FAILED, failed.status());
        assertEquals(1, failed.chunks().size());
    }

    @Test
    void shouldReprocessFailedDocumentToReceived() {
        KnowledgeDocument failed = received().replaceChunks(List.of(text("uno")), LATER).markFailed(LATER);

        KnowledgeDocument received = failed.reprocess(LATER);

        assertEquals(DocumentStatus.RECEIVED, received.status());
        assertTrue(received.chunks().isEmpty());
        assertEquals(failed.content(), received.content());
    }

    @Test
    void shouldRejectReprocessWhenNotFailed() {
        assertThrows(InvalidDocumentException.class, () -> received().reprocess(LATER));
    }

    @Test
    void shouldDeactivateReadyDocument() {
        KnowledgeDocument inactive = ready().deactivate(LATER);

        assertEquals(DocumentStatus.INACTIVE, inactive.status());
        assertEquals(1, inactive.chunks().size());
        assertTrue(inactive.chunks().get(0).embedded());
        assertFalse(inactive.searchable());
    }

    @Test
    void shouldReactivateInactiveDocumentToReady() {
        KnowledgeDocument ready = ready().deactivate(LATER).reactivate(LATER);

        assertEquals(DocumentStatus.READY, ready.status());
        assertTrue(ready.searchable());
        assertEquals(1, ready.chunks().size());
    }

    @Test
    void shouldRejectDeactivateWhenNotReady() {
        assertThrows(InvalidDocumentException.class, () -> received().deactivate(LATER));
    }

    @Test
    void shouldRejectReactivateWhenNotInactive() {
        assertThrows(InvalidDocumentException.class, () -> ready().reactivate(LATER));
    }

    @Test
    void shouldRejectReplaceChunksWhenReady() {
        assertThrows(InvalidDocumentException.class, () -> ready().replaceChunks(List.of(text("nuevo")), LATER));
    }

    @Test
    void shouldRejectReplaceChunksWhenFailed() {
        KnowledgeDocument failed = received().markFailed(LATER);

        assertThrows(InvalidDocumentException.class, () -> failed.replaceChunks(List.of(text("uno")), LATER));
    }

    @Test
    void shouldRejectMarkFailedWhenReady() {
        assertThrows(InvalidDocumentException.class, () -> ready().markFailed(LATER));
    }

    @Test
    void shouldReplaceContentAndDiscardPreviousChunks() {
        KnowledgeDocument ready = ready();
        ChunkId previousChunkId = ready.chunks().get(0).id();
        DocumentContent replacement = new DocumentContent("Texto nuevo para indexar.");

        KnowledgeDocument replaced = ready.replaceContent(replacement, LATER);

        assertEquals(DocumentStatus.RECEIVED, replaced.status());
        assertEquals(replacement, replaced.content());
        assertTrue(replaced.chunks().isEmpty());
        assertEquals(ready.id(), replaced.id());
        assertEquals(ready.title(), replaced.title());
        assertFalse(replaced.searchable());
        assertThrows(InvalidDocumentException.class, () -> replaced.markChunkEmbedded(previousChunkId, LATER));
    }

    @Test
    void shouldReplaceChunksOnChunkedDocumentWithoutKeepingOldEmbeddings() {
        KnowledgeDocument chunked = received().replaceChunks(List.of(text("viejo"), text("otro")), LATER);
        KnowledgeDocument embedded = chunked.markChunkEmbedded(chunked.chunks().get(0).id(), LATER);

        KnowledgeDocument replaced = embedded.replaceChunks(List.of(text("nuevo-a"), text("nuevo-b")), LATER);

        assertEquals(DocumentStatus.CHUNKED, replaced.status());
        assertEquals(2, replaced.chunks().size());
        assertFalse(replaced.chunks().get(0).embedded());
        assertFalse(replaced.chunks().get(1).embedded());
        assertEquals("nuevo-a", replaced.chunks().get(0).text().value());
    }

    @Test
    void shouldRejectReconstituteReceivedWithChunks() {
        KnowledgeChunk chunk = KnowledgeChunk.create(ChunkId.generate(), new ChunkPosition(0), text("uno"));

        assertThrows(
                InvalidDocumentException.class,
                () -> KnowledgeDocument.reconstitute(
                        DocumentId.generate(), TITLE, SOURCE, CONTENT, DocumentStatus.RECEIVED, List.of(chunk), CREATED_AT, LATER));
    }

    @Test
    void shouldRejectReconstituteChunkedWithoutChunks() {
        assertThrows(
                InvalidDocumentException.class,
                () -> KnowledgeDocument.reconstitute(
                        DocumentId.generate(), TITLE, SOURCE, CONTENT, DocumentStatus.CHUNKED, List.of(), CREATED_AT, LATER));
    }

    @Test
    void shouldRejectReconstituteChunkedWhenEveryChunkIsEmbedded() {
        KnowledgeChunk chunk =
                KnowledgeChunk.reconstitute(ChunkId.generate(), new ChunkPosition(0), text("uno"), true);

        assertThrows(
                InvalidDocumentException.class,
                () -> KnowledgeDocument.reconstitute(
                        DocumentId.generate(), TITLE, SOURCE, CONTENT, DocumentStatus.CHUNKED, List.of(chunk), CREATED_AT, LATER));
    }

    @Test
    void shouldRejectReconstituteReadyWhenAChunkIsNotEmbedded() {
        KnowledgeChunk chunk =
                KnowledgeChunk.create(ChunkId.generate(), new ChunkPosition(0), text("uno"));

        assertThrows(
                InvalidDocumentException.class,
                () -> KnowledgeDocument.reconstitute(
                        DocumentId.generate(), TITLE, SOURCE, CONTENT, DocumentStatus.READY, List.of(chunk), CREATED_AT, LATER));
    }

    @Test
    void shouldRejectReconstituteReadyWithoutChunks() {
        assertThrows(
                InvalidDocumentException.class,
                () -> KnowledgeDocument.reconstitute(
                        DocumentId.generate(), TITLE, SOURCE, CONTENT, DocumentStatus.READY, List.of(), CREATED_AT, LATER));
    }

    @Test
    void shouldRejectNonContiguousChunkPositions() {
        KnowledgeChunk first = KnowledgeChunk.create(ChunkId.generate(), new ChunkPosition(0), text("uno"));
        KnowledgeChunk third = KnowledgeChunk.create(ChunkId.generate(), new ChunkPosition(2), text("tres"));

        assertThrows(
                InvalidDocumentException.class,
                () -> KnowledgeDocument.reconstitute(
                        DocumentId.generate(),
                        TITLE,
                        SOURCE,
                        CONTENT,
                        DocumentStatus.CHUNKED,
                        List.of(first, third),
                        CREATED_AT,
                        LATER));
    }

    @Test
    void shouldRejectDuplicateChunkPositions() {
        KnowledgeChunk first = KnowledgeChunk.create(ChunkId.generate(), new ChunkPosition(0), text("uno"));
        KnowledgeChunk duplicate = KnowledgeChunk.create(ChunkId.generate(), new ChunkPosition(0), text("otro"));

        assertThrows(
                InvalidDocumentException.class,
                () -> KnowledgeDocument.reconstitute(
                        DocumentId.generate(),
                        TITLE,
                        SOURCE,
                        CONTENT,
                        DocumentStatus.CHUNKED,
                        List.of(first, duplicate),
                        CREATED_AT,
                        LATER));
    }

    @Test
    void shouldRejectNullChunksOnReconstitute() {
        assertThrows(
                InvalidDocumentException.class,
                () -> KnowledgeDocument.reconstitute(
                        DocumentId.generate(), TITLE, SOURCE, CONTENT, DocumentStatus.RECEIVED, null, CREATED_AT, LATER));
    }

    @Test
    void shouldRejectCreatedAtAfterUpdatedAt() {
        assertThrows(
                InvalidDocumentException.class,
                () -> KnowledgeDocument.reconstitute(
                        DocumentId.generate(),
                        TITLE,
                        SOURCE,
                        CONTENT,
                        DocumentStatus.RECEIVED,
                        List.of(),
                        LATER,
                        CREATED_AT));
    }

    @Test
    void shouldRejectCurrentTimeBeforeCreatedAt() {
        assertThrows(InvalidDocumentException.class, () -> received().replaceContent(CONTENT, CREATED_AT.minusSeconds(1)));
    }

    private static KnowledgeDocument received() {
        return KnowledgeDocument.create(TITLE, SOURCE, CONTENT, CREATED_AT);
    }

    private static KnowledgeDocument ready() {
        KnowledgeDocument chunked = received().replaceChunks(List.of(text("uno")), LATER);
        return chunked.markChunkEmbedded(chunked.chunks().get(0).id(), LATER);
    }

    private static ChunkText text(String value) {
        return new ChunkText(value);
    }
}
