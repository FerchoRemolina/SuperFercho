package com.superfercho.knowledge.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.knowledge.domain.model.ChunkId;
import com.superfercho.knowledge.domain.model.ChunkPosition;
import com.superfercho.knowledge.domain.model.ChunkText;
import com.superfercho.knowledge.domain.model.DocumentContent;
import com.superfercho.knowledge.domain.model.DocumentId;
import com.superfercho.knowledge.domain.model.DocumentSource;
import com.superfercho.knowledge.domain.model.DocumentStatus;
import com.superfercho.knowledge.domain.model.DocumentTitle;
import com.superfercho.knowledge.domain.model.KnowledgeChunkSnapshot;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import com.superfercho.knowledge.infrastructure.persistence.entity.KnowledgeChunkJpaEntity;
import com.superfercho.knowledge.infrastructure.persistence.entity.KnowledgeDocumentJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentPersistenceMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-18T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-18T10:05:00Z");
    private static final DocumentTitle TITLE = new DocumentTitle("Guía de frutas");
    private static final DocumentSource SOURCE = new DocumentSource("manual-interno");
    private static final DocumentContent CONTENT = new DocumentContent("Las frutas deben estar frescas.");
    private static final DocumentId DOCUMENT_ID =
            new DocumentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final ChunkId FIRST_CHUNK_ID =
            new ChunkId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
    private static final ChunkId SECOND_CHUNK_ID =
            new ChunkId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));

    private final KnowledgeDocumentPersistenceMapper mapper = new KnowledgeDocumentPersistenceMapper();

    @Test
    void shouldMapReceivedDocumentWithoutChunks() {
        KnowledgeDocument document = KnowledgeDocument.reconstitute(
                DOCUMENT_ID, TITLE, SOURCE, CONTENT, DocumentStatus.RECEIVED, List.of(), CREATED_AT, UPDATED_AT);

        KnowledgeDocumentJpaEntity entity = mapper.toEntity(document);
        KnowledgeDocument mapped = mapper.toDomain(entity);

        assertEquals(DOCUMENT_ID.value(), entity.getId());
        assertEquals("Guía de frutas", entity.getTitle());
        assertEquals("manual-interno", entity.getSource());
        assertEquals("Las frutas deben estar frescas.", entity.getContent());
        assertEquals(DocumentStatus.RECEIVED, entity.getStatus());
        assertTrue(entity.getChunks().isEmpty());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
        assertEquals(document.id(), mapped.id());
        assertEquals(DocumentStatus.RECEIVED, mapped.status());
        assertTrue(mapped.chunks().isEmpty());
        assertEquals(CREATED_AT, mapped.createdAt());
        assertEquals(UPDATED_AT, mapped.updatedAt());
    }

    @Test
    void shouldMapChunksPreservingIdentityPositionTextAndEmbedded() {
        KnowledgeDocument document = KnowledgeDocument.reconstitute(
                DOCUMENT_ID,
                TITLE,
                SOURCE,
                CONTENT,
                DocumentStatus.CHUNKED,
                List.of(snapshot(FIRST_CHUNK_ID, 0, "uno", true), snapshot(SECOND_CHUNK_ID, 1, "dos", false)),
                CREATED_AT,
                UPDATED_AT);

        KnowledgeDocumentJpaEntity entity = mapper.toEntity(document);
        KnowledgeDocument mapped = mapper.toDomain(entity);

        assertEquals(2, entity.getChunks().size());
        KnowledgeChunkJpaEntity first = entity.getChunks().get(0);
        assertEquals(FIRST_CHUNK_ID.value(), first.getId());
        assertEquals(0, first.getPosition());
        assertEquals("uno", first.getText());
        assertTrue(first.isEmbedded());
        KnowledgeChunkJpaEntity second = entity.getChunks().get(1);
        assertEquals(SECOND_CHUNK_ID.value(), second.getId());
        assertEquals(1, second.getPosition());
        assertEquals("dos", second.getText());
        assertFalse(second.isEmbedded());

        assertEquals(FIRST_CHUNK_ID, mapped.chunks().get(0).id());
        assertEquals(SECOND_CHUNK_ID, mapped.chunks().get(1).id());
        assertEquals(0, mapped.chunks().get(0).position().value());
        assertEquals(1, mapped.chunks().get(1).position().value());
        assertEquals("uno", mapped.chunks().get(0).text().value());
        assertEquals("dos", mapped.chunks().get(1).text().value());
        assertTrue(mapped.chunks().get(0).embedded());
        assertFalse(mapped.chunks().get(1).embedded());
        assertEquals(DocumentStatus.CHUNKED, mapped.status());
    }

    @Test
    void shouldMapReadyFailedAndInactiveStatuses() {
        assertEquals(
                DocumentStatus.READY,
                mapper.toDomain(mapper.toEntity(document(DocumentStatus.READY, true))).status());
        assertEquals(
                DocumentStatus.FAILED,
                mapper.toDomain(mapper.toEntity(document(DocumentStatus.FAILED, false))).status());
        assertEquals(
                DocumentStatus.INACTIVE,
                mapper.toDomain(mapper.toEntity(document(DocumentStatus.INACTIVE, true))).status());
    }

    private static KnowledgeDocument document(DocumentStatus status, boolean embedded) {
        return KnowledgeDocument.reconstitute(
                DOCUMENT_ID,
                TITLE,
                SOURCE,
                CONTENT,
                status,
                List.of(snapshot(FIRST_CHUNK_ID, 0, "uno", embedded)),
                CREATED_AT,
                UPDATED_AT);
    }

    private static KnowledgeChunkSnapshot snapshot(ChunkId id, int position, String text, boolean embedded) {
        return new KnowledgeChunkSnapshot(id, new ChunkPosition(position), new ChunkText(text), embedded);
    }
}
