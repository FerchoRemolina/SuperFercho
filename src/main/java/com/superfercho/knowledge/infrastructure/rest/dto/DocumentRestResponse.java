package com.superfercho.knowledge.infrastructure.rest.dto;

import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.domain.model.DocumentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentRestResponse(
        UUID id,
        String title,
        String source,
        String content,
        DocumentStatus status,
        List<ChunkRestResponse> chunks,
        Instant createdAt,
        Instant updatedAt) {

    public static DocumentRestResponse from(DocumentResult result) {
        return new DocumentRestResponse(
                result.id(),
                result.title(),
                result.source(),
                result.content(),
                result.status(),
                result.chunks().stream().map(ChunkRestResponse::from).toList(),
                result.createdAt(),
                result.updatedAt());
    }
}
