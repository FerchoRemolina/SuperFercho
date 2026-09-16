package com.superfercho.knowledge.application.dto;

import com.superfercho.knowledge.domain.model.KnowledgeChunk;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import com.superfercho.knowledge.domain.model.DocumentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentResult(
        UUID id,
        String title,
        String source,
        String content,
        DocumentStatus status,
        List<ChunkResult> chunks,
        Instant createdAt,
        Instant updatedAt) {

    public static DocumentResult from(KnowledgeDocument document) {
        List<ChunkResult> chunks = document.chunks().stream().map(DocumentResult::toChunk).toList();
        return new DocumentResult(
                document.id().value(),
                document.title().value(),
                document.source().value(),
                document.content().value(),
                document.status(),
                chunks,
                document.createdAt(),
                document.updatedAt());
    }

    private static ChunkResult toChunk(KnowledgeChunk chunk) {
        return new ChunkResult(
                chunk.id().value(), chunk.position().value(), chunk.text().value(), chunk.embedded());
    }
}
