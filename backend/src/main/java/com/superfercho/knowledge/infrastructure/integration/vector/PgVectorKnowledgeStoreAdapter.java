package com.superfercho.knowledge.infrastructure.integration.vector;

import com.superfercho.knowledge.application.dto.ChunkEmbedding;
import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.dto.KnowledgeSearchHit;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.application.port.KnowledgeVectorStorePort;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL + pgvector store. Search {@code score} is cosine distance ({@code <=>}); lower is closer.
 */
@Component
@Profile("!test")
public class PgVectorKnowledgeStoreAdapter implements KnowledgeVectorStorePort {

    static final int EMBEDDING_DIMENSIONS = 1536;

    private static final String UPSERT_SQL =
            """
            insert into knowledge.document_embeddings (chunk_id, document_id, position, embedding)
            values (?, ?, ?, cast(? as vector))
            on conflict (chunk_id) do update set
                document_id = excluded.document_id,
                position = excluded.position,
                embedding = excluded.embedding
            """;

    private static final String DELETE_SQL =
            "delete from knowledge.document_embeddings where document_id = ?";

    private static final String SEARCH_SQL =
            """
            select d.id as document_id,
                   c.id as chunk_id,
                   d.title,
                   d.source,
                   c.text as chunk_text,
                   (e.embedding <=> cast(? as vector)) as score
            from knowledge.document_embeddings e
            inner join knowledge.document_chunks c on c.id = e.chunk_id
            inner join knowledge.documents d on d.id = e.document_id and d.id = c.document_id
            where d.status = 'READY'
            order by score
            limit ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PgVectorKnowledgeStoreAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(ChunkEmbedding embedding) {
        if (embedding == null || embedding.chunkId() == null || embedding.documentId() == null) {
            throw new KnowledgeProcessingException("chunk embedding cannot be null");
        }
        String vector = toVectorLiteral(embedding.embedding());
        try {
            jdbcTemplate.update(
                    UPSERT_SQL,
                    embedding.chunkId(),
                    embedding.documentId(),
                    embedding.position(),
                    vector);
        } catch (DataAccessException exception) {
            throw new KnowledgeProcessingException("failed to store chunk embedding");
        }
    }

    @Override
    public void deleteByDocumentId(UUID documentId) {
        if (documentId == null) {
            throw new KnowledgeProcessingException("document id cannot be null");
        }
        try {
            jdbcTemplate.update(DELETE_SQL, documentId);
        } catch (DataAccessException exception) {
            throw new KnowledgeProcessingException("failed to delete document embeddings");
        }
    }

    @Override
    public List<KnowledgeSearchHit> search(EmbeddingVector query, int limit) {
        if (limit <= 0) {
            throw new KnowledgeProcessingException("limit must be positive");
        }
        String vector = toVectorLiteral(query);
        try {
            return jdbcTemplate.query(
                    SEARCH_SQL,
                    (resultSet, rowNum) -> new KnowledgeSearchHit(
                            resultSet.getObject("document_id", UUID.class),
                            resultSet.getObject("chunk_id", UUID.class),
                            resultSet.getString("title"),
                            resultSet.getString("source"),
                            resultSet.getString("chunk_text"),
                            resultSet.getDouble("score")),
                    vector,
                    limit);
        } catch (DataAccessException exception) {
            throw new KnowledgeProcessingException("failed to search knowledge");
        }
    }

    private static String toVectorLiteral(EmbeddingVector embedding) {
        if (embedding == null) {
            throw new KnowledgeProcessingException("embedding cannot be null or empty");
        }
        float[] values = embedding.values();
        if (values.length != EMBEDDING_DIMENSIONS) {
            throw new KnowledgeProcessingException("embedding dimension must be " + EMBEDDING_DIMENSIONS);
        }
        StringBuilder literal = new StringBuilder(values.length * 8);
        literal.append('[');
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                literal.append(',');
            }
            literal.append(String.format(Locale.ROOT, "%.8f", values[index]));
        }
        literal.append(']');
        return literal.toString();
    }
}
