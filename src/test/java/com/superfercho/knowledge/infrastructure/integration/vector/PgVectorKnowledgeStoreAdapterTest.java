package com.superfercho.knowledge.infrastructure.integration.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.knowledge.application.dto.ChunkEmbedding;
import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.dto.KnowledgeSearchHit;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.application.port.KnowledgeVectorStorePort;
import com.superfercho.knowledge.domain.model.ChunkText;
import com.superfercho.knowledge.domain.model.DocumentContent;
import com.superfercho.knowledge.domain.model.DocumentSource;
import com.superfercho.knowledge.domain.model.DocumentStatus;
import com.superfercho.knowledge.domain.model.DocumentTitle;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PgVectorKnowledgeStoreAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-18T16:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-18T16:05:00Z");
    private static final Instant CHANGED_AT = Instant.parse("2026-09-18T16:10:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                            DockerImageName.parse("pgvector/pgvector:pg16")
                                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("superfercho")
                    .withUsername("superfercho")
                    .withPassword("superfercho");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("superfercho.security.jwt.secret", () -> "test-only-superfercho-jwt-secret-key-32b");
    }

    @Autowired
    private KnowledgeVectorStorePort vectorStore;

    @Autowired
    private KnowledgeDocumentRepository documentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUpsertSingleVectorAndSearchReadyDocument() {
        KnowledgeDocument document = documentRepository.save(ready("manzana"));
        UUID chunkId = document.chunks().get(0).id().value();

        vectorStore.upsert(new ChunkEmbedding(document.id().value(), chunkId, 0, basis(0)));

        List<KnowledgeSearchHit> hits = vectorStore.search(basis(0), 5);

        KnowledgeSearchHit hit = byChunkId(hits, chunkId);
        assertThat(hit.documentId()).isEqualTo(document.id().value());
        assertThat(hit.chunkId()).isEqualTo(chunkId);
        assertThat(hit.title()).isEqualTo("Guía de frutas");
        assertThat(hit.source()).isEqualTo("manual-interno");
        assertThat(hit.chunkText()).isEqualTo("manzana");
        assertThat(hit.score()).isEqualTo(0.0);
        assertThat(embeddingCount(document.id().value())).isEqualTo(1);
    }

    @Test
    void shouldUpsertSeveralVectorsAndOrderByCosineDistance() {
        KnowledgeDocument document = documentRepository.save(ready("cerca", "lejos"));
        UUID closeId = document.chunks().get(0).id().value();
        UUID farId = document.chunks().get(1).id().value();

        vectorStore.upsert(new ChunkEmbedding(document.id().value(), closeId, 0, basis(20)));
        vectorStore.upsert(new ChunkEmbedding(document.id().value(), farId, 1, basis(21)));

        List<KnowledgeSearchHit> hits = vectorStore.search(basis(20), 50);
        List<KnowledgeSearchHit> documentHits = hits.stream()
                .filter(hit -> hit.documentId().equals(document.id().value()))
                .toList();

        assertThat(documentHits).hasSize(2);
        assertThat(documentHits.get(0).chunkId()).isEqualTo(closeId);
        assertThat(documentHits.get(1).chunkId()).isEqualTo(farId);
        assertThat(documentHits.get(0).score()).isLessThan(documentHits.get(1).score());
        assertThat(documentHits.get(0).chunkText()).isEqualTo("cerca");
        assertThat(documentHits.get(1).chunkText()).isEqualTo("lejos");
    }

    @Test
    void shouldUpdateExistingChunkEmbeddingWithoutDuplicating() {
        KnowledgeDocument document = documentRepository.save(ready("original"));
        UUID chunkId = document.chunks().get(0).id().value();
        UUID documentId = document.id().value();

        vectorStore.upsert(new ChunkEmbedding(documentId, chunkId, 0, basis(0)));
        vectorStore.upsert(new ChunkEmbedding(documentId, chunkId, 0, basis(3)));

        List<KnowledgeSearchHit> hits = vectorStore.search(basis(3), 5);
        KnowledgeSearchHit hit = byChunkId(hits, chunkId);
        assertThat(hit.score()).isEqualTo(0.0);
        assertThat(embeddingCount(documentId)).isEqualTo(1);
        assertThat(storedChunkId(documentId)).isEqualTo(chunkId);
        assertThat(storedPosition(documentId)).isEqualTo(0);
    }

    @Test
    void shouldDeleteEmbeddingsOfOneDocumentWithoutAffectingOthers() {
        KnowledgeDocument keep = documentRepository.save(ready("permanece"));
        KnowledgeDocument remove = documentRepository.save(ready("eliminar"));
        vectorStore.upsert(new ChunkEmbedding(
                keep.id().value(), keep.chunks().get(0).id().value(), 0, basis(1)));
        vectorStore.upsert(new ChunkEmbedding(
                remove.id().value(), remove.chunks().get(0).id().value(), 0, basis(1)));

        vectorStore.deleteByDocumentId(remove.id().value());

        assertThat(embeddingCount(remove.id().value())).isZero();
        assertThat(embeddingCount(keep.id().value())).isEqualTo(1);
        assertThat(documentRepository.findById(remove.id().value())).isPresent();
        assertThat(chunkCount(remove.id().value())).isEqualTo(1);
        assertThat(vectorStore.search(basis(1), 20).stream()
                        .map(KnowledgeSearchHit::documentId)
                        .toList())
                .contains(keep.id().value())
                .doesNotContain(remove.id().value());
    }

    @Test
    void shouldRespectSearchLimit() {
        KnowledgeDocument document = documentRepository.save(ready("uno", "dos", "tres"));
        UUID documentId = document.id().value();
        vectorStore.upsert(new ChunkEmbedding(documentId, document.chunks().get(0).id().value(), 0, basis(2)));
        vectorStore.upsert(new ChunkEmbedding(documentId, document.chunks().get(1).id().value(), 1, basis(2)));
        vectorStore.upsert(new ChunkEmbedding(documentId, document.chunks().get(2).id().value(), 2, basis(2)));

        List<KnowledgeSearchHit> hits = vectorStore.search(basis(2), 2);

        assertThat(hits).hasSize(2);
        assertThat(hits).allMatch(hit -> hit.documentId().equals(documentId));
    }

    @Test
    void shouldSearchAcrossMultipleReadyDocuments() {
        KnowledgeDocument fruits = documentRepository.save(readyWithTitle("Frutas", "manzana"));
        KnowledgeDocument vegetables = documentRepository.save(readyWithTitle("Verduras", "zanahoria"));
        vectorStore.upsert(new ChunkEmbedding(
                fruits.id().value(), fruits.chunks().get(0).id().value(), 0, basis(4)));
        vectorStore.upsert(new ChunkEmbedding(
                vegetables.id().value(), vegetables.chunks().get(0).id().value(), 0, basis(4)));

        List<UUID> documentIds = vectorStore.search(basis(4), 10).stream()
                .map(KnowledgeSearchHit::documentId)
                .toList();

        assertThat(documentIds).contains(fruits.id().value(), vegetables.id().value());
    }

    @Test
    void shouldExcludeReceivedChunkedFailedAndInactiveDocuments() {
        KnowledgeDocument ready = documentRepository.save(ready("visible"));
        KnowledgeDocument chunked = documentRepository.save(chunked("chunked"));
        KnowledgeDocument failed = documentRepository.save(chunked("failed").markFailed(UPDATED_AT));
        KnowledgeDocument inactive = documentRepository.save(ready("inactive").deactivate(CHANGED_AT));
        UUID receivedId = insertDocument("RECEIVED");
        UUID receivedChunkId = insertChunk(receivedId, 0, "received");

        vectorStore.upsert(new ChunkEmbedding(ready.id().value(), ready.chunks().get(0).id().value(), 0, basis(5)));
        vectorStore.upsert(new ChunkEmbedding(chunked.id().value(), chunked.chunks().get(0).id().value(), 0, basis(5)));
        vectorStore.upsert(new ChunkEmbedding(failed.id().value(), failed.chunks().get(0).id().value(), 0, basis(5)));
        vectorStore.upsert(
                new ChunkEmbedding(inactive.id().value(), inactive.chunks().get(0).id().value(), 0, basis(5)));
        vectorStore.upsert(new ChunkEmbedding(receivedId, receivedChunkId, 0, basis(5)));

        List<UUID> documentIds = vectorStore.search(basis(5), 20).stream()
                .map(KnowledgeSearchHit::documentId)
                .toList();

        assertThat(documentIds).contains(ready.id().value());
        assertThat(documentIds)
                .doesNotContain(
                        chunked.id().value(), failed.id().value(), inactive.id().value(), receivedId);
    }

    @Test
    void shouldReturnInactiveDocumentAfterReactivationWithoutReembedding() {
        KnowledgeDocument ready = documentRepository.save(ready("reactivable"));
        UUID documentId = ready.id().value();
        UUID chunkId = ready.chunks().get(0).id().value();
        vectorStore.upsert(new ChunkEmbedding(documentId, chunkId, 0, basis(6)));

        documentRepository.save(ready.deactivate(CHANGED_AT));
        assertThat(vectorStore.search(basis(6), 50).stream().map(KnowledgeSearchHit::documentId))
                .doesNotContain(documentId);

        documentRepository.save(documentRepository.findById(documentId).orElseThrow().reactivate(CHANGED_AT));
        assertThat(documentRepository.findById(documentId).orElseThrow().status())
                .isEqualTo(DocumentStatus.READY);
        List<KnowledgeSearchHit> hits = vectorStore.search(basis(6), 50);
        KnowledgeSearchHit hit = byChunkId(hits, chunkId);
        assertThat(hit.documentId()).isEqualTo(documentId);
        assertThat(embeddingCount(documentId)).isEqualTo(1);
    }

    @Test
    void shouldPreserveDocumentIdChunkIdAndPosition() {
        KnowledgeDocument document = documentRepository.save(ready("uno", "dos"));
        UUID documentId = document.id().value();
        UUID firstChunkId = document.chunks().get(0).id().value();
        UUID secondChunkId = document.chunks().get(1).id().value();

        vectorStore.upsert(new ChunkEmbedding(documentId, firstChunkId, 0, basis(7)));
        vectorStore.upsert(new ChunkEmbedding(documentId, secondChunkId, 1, basis(8)));

        assertThat(storedChunkIds(documentId)).containsExactlyInAnyOrder(firstChunkId, secondChunkId);
        assertThat(storedPositionFor(firstChunkId)).isEqualTo(0);
        assertThat(storedPositionFor(secondChunkId)).isEqualTo(1);
        assertThat(storedDocumentId(firstChunkId)).isEqualTo(documentId);
    }

    @Test
    void shouldRejectInvalidEmbeddingDimension() {
        KnowledgeDocument document = documentRepository.save(ready("dimension"));
        UUID documentId = document.id().value();
        UUID chunkId = document.chunks().get(0).id().value();

        assertThatThrownBy(() -> vectorStore.upsert(new ChunkEmbedding(
                        documentId, chunkId, 0, new EmbeddingVector(new float[] {0.1f, 0.2f}))))
                .isInstanceOf(KnowledgeProcessingException.class)
                .hasMessage("embedding dimension must be 1536");
        assertThatThrownBy(() -> vectorStore.search(new EmbeddingVector(new float[] {0.1f}), 1))
                .isInstanceOf(KnowledgeProcessingException.class)
                .hasMessage("embedding dimension must be 1536");
        assertThat(embeddingCount(documentId)).isZero();
    }

    private static KnowledgeDocument ready(String... texts) {
        return readyWithTitle("Guía de frutas", texts);
    }

    private static KnowledgeDocument readyWithTitle(String title, String... texts) {
        KnowledgeDocument chunked = KnowledgeDocument.create(
                        new DocumentTitle(title),
                        new DocumentSource("manual-interno"),
                        new DocumentContent("Las frutas deben estar frescas."),
                        CREATED_AT)
                .replaceChunks(List.of(texts).stream().map(ChunkText::new).toList(), UPDATED_AT);
        KnowledgeDocument current = chunked;
        for (var chunk : chunked.chunks()) {
            current = current.markChunkEmbedded(chunk.id(), UPDATED_AT);
        }
        return current;
    }

    private static KnowledgeDocument chunked(String text) {
        return KnowledgeDocument.create(
                        new DocumentTitle("Guía de frutas"),
                        new DocumentSource("manual-interno"),
                        new DocumentContent("Las frutas deben estar frescas."),
                        CREATED_AT)
                .replaceChunks(List.of(new ChunkText(text)), UPDATED_AT);
    }

    private static EmbeddingVector basis(int index) {
        float[] values = new float[PgVectorKnowledgeStoreAdapter.EMBEDDING_DIMENSIONS];
        values[index] = 1.0f;
        return new EmbeddingVector(values);
    }

    private static KnowledgeSearchHit byChunkId(List<KnowledgeSearchHit> hits, UUID chunkId) {
        return hits.stream().filter(hit -> hit.chunkId().equals(chunkId)).findFirst().orElseThrow();
    }

    private int embeddingCount(UUID documentId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from knowledge.document_embeddings where document_id = ?",
                Integer.class,
                documentId);
        return count == null ? 0 : count;
    }

    private int chunkCount(UUID documentId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from knowledge.document_chunks where document_id = ?", Integer.class, documentId);
        return count == null ? 0 : count;
    }

    private UUID storedChunkId(UUID documentId) {
        return jdbcTemplate.queryForObject(
                "select chunk_id from knowledge.document_embeddings where document_id = ?", UUID.class, documentId);
    }

    private List<UUID> storedChunkIds(UUID documentId) {
        return jdbcTemplate.queryForList(
                "select chunk_id from knowledge.document_embeddings where document_id = ?", UUID.class, documentId);
    }

    private int storedPosition(UUID documentId) {
        Integer position = jdbcTemplate.queryForObject(
                "select position from knowledge.document_embeddings where document_id = ?",
                Integer.class,
                documentId);
        return position == null ? -1 : position;
    }

    private int storedPositionFor(UUID chunkId) {
        Integer position = jdbcTemplate.queryForObject(
                "select position from knowledge.document_embeddings where chunk_id = ?", Integer.class, chunkId);
        return position == null ? -1 : position;
    }

    private UUID storedDocumentId(UUID chunkId) {
        return jdbcTemplate.queryForObject(
                "select document_id from knowledge.document_embeddings where chunk_id = ?", UUID.class, chunkId);
    }

    private UUID insertDocument(String status) {
        UUID documentId = UUID.randomUUID();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    insert into knowledge.documents (
                        id, title, source, content, status, created_at, updated_at
                    ) values (?, 'bypass', 'source', 'content', ?, ?, ?)
                    """);
            statement.setObject(1, documentId);
            statement.setString(2, status);
            statement.setTimestamp(3, Timestamp.from(CREATED_AT));
            statement.setTimestamp(4, Timestamp.from(CREATED_AT));
            return statement;
        });
        return documentId;
    }

    private UUID insertChunk(UUID documentId, int position, String text) {
        UUID chunkId = UUID.randomUUID();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    insert into knowledge.document_chunks (
                        id, document_id, position, text, embedded
                    ) values (?, ?, ?, ?, false)
                    """);
            statement.setObject(1, chunkId);
            statement.setObject(2, documentId);
            statement.setInt(3, position);
            statement.setString(4, text);
            return statement;
        });
        return chunkId;
    }
}
