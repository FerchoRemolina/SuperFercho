package com.superfercho.knowledge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.domain.model.ChunkText;
import com.superfercho.knowledge.domain.model.DocumentContent;
import com.superfercho.knowledge.domain.model.DocumentSource;
import com.superfercho.knowledge.domain.model.DocumentStatus;
import com.superfercho.knowledge.domain.model.DocumentTitle;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import com.superfercho.knowledge.infrastructure.persistence.entity.KnowledgeChunkJpaEntity;
import com.superfercho.knowledge.infrastructure.persistence.entity.KnowledgeDocumentJpaEntity;
import com.superfercho.knowledge.infrastructure.persistence.repository.KnowledgeDocumentJpaRepository;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeDocumentPersistenceAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-18T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-18T10:05:00Z");
    private static final Instant REPLACED_AT = Instant.parse("2026-09-18T10:10:00Z");
    private static final DocumentTitle TITLE = new DocumentTitle("Guía de frutas");
    private static final DocumentSource SOURCE = new DocumentSource("manual-interno");
    private static final DocumentContent CONTENT = new DocumentContent("Las frutas deben estar frescas.");

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
    private KnowledgeDocumentRepository documentRepository;

    @Autowired
    private KnowledgeDocumentJpaRepository documentJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldPersistReceivedDocumentWithoutChunks() {
        KnowledgeDocument saved = documentRepository.save(received());

        KnowledgeDocument loaded = documentRepository.findById(saved.id().value()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.title()).isEqualTo(TITLE);
        assertThat(loaded.source()).isEqualTo(SOURCE);
        assertThat(loaded.content()).isEqualTo(CONTENT);
        assertThat(loaded.status()).isEqualTo(DocumentStatus.RECEIVED);
        assertThat(loaded.chunks()).isEmpty();
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.updatedAt()).isEqualTo(CREATED_AT);
        assertThat(chunkCount(saved.id().value())).isZero();
    }

    @Test
    void shouldPersistAndReloadMultipleChunksPreservingIdentityAndOrder() {
        KnowledgeDocument saved = documentRepository.save(chunked("uno", "dos", "tres"));
        KnowledgeDocument loaded = documentRepository.findById(saved.id().value()).orElseThrow();

        assertThat(loaded.status()).isEqualTo(DocumentStatus.CHUNKED);
        assertThat(loaded.chunks()).hasSize(3);
        assertThat(loaded.chunks().get(0).id()).isEqualTo(saved.chunks().get(0).id());
        assertThat(loaded.chunks().get(1).id()).isEqualTo(saved.chunks().get(1).id());
        assertThat(loaded.chunks().get(2).id()).isEqualTo(saved.chunks().get(2).id());
        assertThat(loaded.chunks().get(0).position().value()).isEqualTo(0);
        assertThat(loaded.chunks().get(1).position().value()).isEqualTo(1);
        assertThat(loaded.chunks().get(2).position().value()).isEqualTo(2);
        assertThat(loaded.chunks().get(0).text().value()).isEqualTo("uno");
        assertThat(loaded.chunks().get(1).text().value()).isEqualTo("dos");
        assertThat(loaded.chunks().get(2).text().value()).isEqualTo("tres");
        assertThat(loaded.chunks().get(0).embedded()).isFalse();
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void shouldReturnEmptyWhenDocumentDoesNotExist() {
        assertThat(documentRepository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldFindAllWithoutMixingChunksAcrossDocuments() {
        KnowledgeDocument fruits = documentRepository.save(chunked("manzana", "pera"));
        KnowledgeDocument vegetables = documentRepository.save(chunked("zanahoria"));

        List<KnowledgeDocument> loaded = documentRepository.findAll();

        KnowledgeDocument loadedFruits = byId(loaded, fruits.id().value());
        KnowledgeDocument loadedVegetables = byId(loaded, vegetables.id().value());
        assertThat(loadedFruits.chunks()).hasSize(2);
        assertThat(loadedFruits.chunks().get(0).text().value()).isEqualTo("manzana");
        assertThat(loadedFruits.chunks().get(1).text().value()).isEqualTo("pera");
        assertThat(loadedVegetables.chunks()).hasSize(1);
        assertThat(loadedVegetables.chunks().get(0).text().value()).isEqualTo("zanahoria");
        assertThat(loadedFruits.chunks().get(0).id()).isNotEqualTo(loadedVegetables.chunks().get(0).id());
    }

    @Test
    void shouldPersistReadyFailedAndInactiveStatuses() {
        KnowledgeDocument ready = documentRepository.save(ready("listo"));
        KnowledgeDocument failed = documentRepository.save(chunked("fallo").markFailed(UPDATED_AT));
        KnowledgeDocument inactive = documentRepository.save(ready("inactivo").deactivate(REPLACED_AT));

        assertThat(documentRepository.findById(ready.id().value()).orElseThrow().status())
                .isEqualTo(DocumentStatus.READY);
        assertThat(documentRepository.findById(ready.id().value()).orElseThrow().chunks().get(0).embedded())
                .isTrue();
        assertThat(documentRepository.findById(failed.id().value()).orElseThrow().status())
                .isEqualTo(DocumentStatus.FAILED);
        assertThat(documentRepository.findById(failed.id().value()).orElseThrow().chunks().get(0).embedded())
                .isFalse();
        KnowledgeDocument loadedInactive = documentRepository.findById(inactive.id().value()).orElseThrow();
        assertThat(loadedInactive.status()).isEqualTo(DocumentStatus.INACTIVE);
        assertThat(loadedInactive.chunks().get(0).embedded()).isTrue();
        assertThat(loadedInactive.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loadedInactive.updatedAt()).isEqualTo(REPLACED_AT);
    }

    @Test
    void shouldReplaceContentAndRemovePreviousChunks() {
        KnowledgeDocument saved = documentRepository.save(ready("viejo"));
        UUID previousChunkId = saved.chunks().get(0).id().value();

        KnowledgeDocument replaced = documentRepository.save(
                saved.replaceContent(new DocumentContent("Texto nuevo para indexar."), REPLACED_AT));
        KnowledgeDocument loaded = documentRepository.findById(replaced.id().value()).orElseThrow();

        assertThat(loaded.status()).isEqualTo(DocumentStatus.RECEIVED);
        assertThat(loaded.content().value()).isEqualTo("Texto nuevo para indexar.");
        assertThat(loaded.chunks()).isEmpty();
        assertThat(loaded.updatedAt()).isEqualTo(REPLACED_AT);
        assertThat(chunkCount(loaded.id().value())).isZero();
        assertThat(chunkExists(previousChunkId)).isFalse();
    }

    @Test
    void shouldReplaceChunksAndOrphanPreviousOnes() {
        KnowledgeDocument saved = documentRepository.save(chunked("viejo-a", "viejo-b"));
        UUID previousFirstId = saved.chunks().get(0).id().value();
        UUID previousSecondId = saved.chunks().get(1).id().value();

        KnowledgeDocument replaced = documentRepository.save(
                saved.replaceChunks(List.of(new ChunkText("nuevo-a"), new ChunkText("nuevo-b")), REPLACED_AT));
        KnowledgeDocument loaded = documentRepository.findById(replaced.id().value()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.title()).isEqualTo(TITLE);
        assertThat(loaded.source()).isEqualTo(SOURCE);
        assertThat(loaded.content()).isEqualTo(CONTENT);
        assertThat(loaded.status()).isEqualTo(DocumentStatus.CHUNKED);
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.updatedAt()).isEqualTo(REPLACED_AT);
        assertThat(loaded.chunks()).hasSize(2);
        assertThat(loaded.chunks().get(0).id()).isEqualTo(replaced.chunks().get(0).id());
        assertThat(loaded.chunks().get(1).id()).isEqualTo(replaced.chunks().get(1).id());
        assertThat(loaded.chunks().get(0).id().value()).isNotEqualTo(previousFirstId);
        assertThat(loaded.chunks().get(1).id().value()).isNotEqualTo(previousSecondId);
        assertThat(loaded.chunks().get(0).position().value()).isEqualTo(0);
        assertThat(loaded.chunks().get(1).position().value()).isEqualTo(1);
        assertThat(loaded.chunks().get(0).text().value()).isEqualTo("nuevo-a");
        assertThat(loaded.chunks().get(1).text().value()).isEqualTo("nuevo-b");
        assertThat(chunkExists(previousFirstId)).isFalse();
        assertThat(chunkExists(previousSecondId)).isFalse();
        assertThat(chunkCount(loaded.id().value())).isEqualTo(2);
        assertThat(chunkIds(loaded.id().value()))
                .containsExactly(replaced.chunks().get(0).id().value(), replaced.chunks().get(1).id().value());
    }

    @Test
    void shouldRollbackReplacedChunksWhenSecondFlushFails() {
        KnowledgeDocument saved = documentRepository.save(chunked("viejo-a", "viejo-b"));
        UUID documentId = saved.id().value();
        UUID previousFirstId = saved.chunks().get(0).id().value();
        UUID previousSecondId = saved.chunks().get(1).id().value();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    KnowledgeDocumentJpaEntity existing =
                            documentJpaRepository.findById(documentId).orElseThrow();
                    existing.getChunks().clear();
                    documentJpaRepository.flush();
                    existing.getChunks()
                            .add(new KnowledgeChunkJpaEntity(UUID.randomUUID(), 0, "   ", false));
                    documentJpaRepository.saveAndFlush(existing);
                }))
                .isInstanceOf(DataIntegrityViolationException.class);

        KnowledgeDocument loaded = documentRepository.findById(documentId).orElseThrow();
        assertThat(loaded.status()).isEqualTo(DocumentStatus.CHUNKED);
        assertThat(loaded.chunks()).hasSize(2);
        assertThat(loaded.chunks().get(0).id().value()).isEqualTo(previousFirstId);
        assertThat(loaded.chunks().get(1).id().value()).isEqualTo(previousSecondId);
        assertThat(loaded.chunks().get(0).text().value()).isEqualTo("viejo-a");
        assertThat(loaded.chunks().get(1).text().value()).isEqualTo("viejo-b");
        assertThat(chunkExists(previousFirstId)).isTrue();
        assertThat(chunkExists(previousSecondId)).isTrue();
        assertThat(chunkCount(documentId)).isEqualTo(2);
    }

    @Test
    void shouldCascadeDeleteChunksWhenDocumentIsDeleted() {
        KnowledgeDocument saved = documentRepository.save(chunked("uno", "dos"));
        UUID documentId = saved.id().value();
        UUID chunkId = saved.chunks().get(0).id().value();

        documentJpaRepository.deleteById(documentId);
        documentJpaRepository.flush();

        assertThat(documentRepository.findById(documentId)).isEmpty();
        assertThat(chunkExists(chunkId)).isFalse();
        assertThat(chunkCount(documentId)).isZero();
    }

    @Test
    void shouldRejectInvalidStatusAtDatabase() {
        assertThatThrownBy(() -> insertDocumentBypassingDomain("INDEXING"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNegativeChunkPositionAtDatabase() {
        UUID documentId = insertDocumentBypassingDomain("RECEIVED");

        assertThatThrownBy(() -> insertChunkBypassingDomain(documentId, -1, "uno"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectBlankChunkTextAtDatabase() {
        UUID documentId = insertDocumentBypassingDomain("RECEIVED");

        assertThatThrownBy(() -> insertChunkBypassingDomain(documentId, 0, "   "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static KnowledgeDocument received() {
        return KnowledgeDocument.create(TITLE, SOURCE, CONTENT, CREATED_AT);
    }

    private static KnowledgeDocument chunked(String... texts) {
        return received().replaceChunks(List.of(texts).stream().map(ChunkText::new).toList(), UPDATED_AT);
    }

    private static KnowledgeDocument ready(String text) {
        KnowledgeDocument chunked = chunked(text);
        return chunked.markChunkEmbedded(chunked.chunks().get(0).id(), UPDATED_AT);
    }

    private static KnowledgeDocument byId(List<KnowledgeDocument> documents, UUID id) {
        return documents.stream()
                .filter(document -> document.id().value().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private int chunkCount(UUID documentId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from knowledge.document_chunks where document_id = ?", Integer.class, documentId);
        return count == null ? 0 : count;
    }

    private boolean chunkExists(UUID chunkId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from knowledge.document_chunks where id = ?", Integer.class, chunkId);
        return count != null && count > 0;
    }

    private List<UUID> chunkIds(UUID documentId) {
        return jdbcTemplate.queryForList(
                "select id from knowledge.document_chunks where document_id = ? order by position",
                UUID.class,
                documentId);
    }

    private UUID insertDocumentBypassingDomain(String status) {
        UUID documentId = UUID.randomUUID();
        jdbcTemplate.update(
                connection -> {
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

    private void insertChunkBypassingDomain(UUID documentId, int position, String text) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into knowledge.document_chunks (
                                id, document_id, position, text, embedded
                            ) values (?, ?, ?, ?, false)
                            """);
                    statement.setObject(1, UUID.randomUUID());
                    statement.setObject(2, documentId);
                    statement.setInt(3, position);
                    statement.setString(4, text);
                    return statement;
                });
    }
}
