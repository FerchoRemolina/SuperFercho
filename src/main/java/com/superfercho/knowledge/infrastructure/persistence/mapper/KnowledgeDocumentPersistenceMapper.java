package com.superfercho.knowledge.infrastructure.persistence.mapper;

import com.superfercho.knowledge.domain.model.ChunkId;
import com.superfercho.knowledge.domain.model.ChunkPosition;
import com.superfercho.knowledge.domain.model.ChunkText;
import com.superfercho.knowledge.domain.model.DocumentContent;
import com.superfercho.knowledge.domain.model.DocumentId;
import com.superfercho.knowledge.domain.model.DocumentSource;
import com.superfercho.knowledge.domain.model.DocumentTitle;
import com.superfercho.knowledge.domain.model.KnowledgeChunk;
import com.superfercho.knowledge.domain.model.KnowledgeChunkSnapshot;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import com.superfercho.knowledge.infrastructure.persistence.entity.KnowledgeChunkJpaEntity;
import com.superfercho.knowledge.infrastructure.persistence.entity.KnowledgeDocumentJpaEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeDocumentPersistenceMapper {

    public KnowledgeDocumentJpaEntity toEntity(KnowledgeDocument document) {
        List<KnowledgeChunkJpaEntity> chunks =
                document.chunks().stream().map(this::toChunkEntity).toList();
        return new KnowledgeDocumentJpaEntity(
                document.id().value(),
                document.title().value(),
                document.source().value(),
                document.content().value(),
                document.status(),
                document.createdAt(),
                document.updatedAt(),
                chunks);
    }

    public KnowledgeDocument toDomain(KnowledgeDocumentJpaEntity entity) {
        List<KnowledgeChunkSnapshot> chunks =
                entity.getChunks().stream().map(this::toChunkSnapshot).toList();
        return KnowledgeDocument.reconstitute(
                new DocumentId(entity.getId()),
                new DocumentTitle(entity.getTitle()),
                new DocumentSource(entity.getSource()),
                new DocumentContent(entity.getContent()),
                entity.getStatus(),
                chunks,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private KnowledgeChunkJpaEntity toChunkEntity(KnowledgeChunk chunk) {
        return new KnowledgeChunkJpaEntity(
                chunk.id().value(), chunk.position().value(), chunk.text().value(), chunk.embedded());
    }

    private KnowledgeChunkSnapshot toChunkSnapshot(KnowledgeChunkJpaEntity entity) {
        return new KnowledgeChunkSnapshot(
                new ChunkId(entity.getId()),
                new ChunkPosition(entity.getPosition()),
                new ChunkText(entity.getText()),
                entity.isEmbedded());
    }
}
