package com.superfercho.knowledge.infrastructure.rest.dto;

import com.superfercho.knowledge.application.dto.KnowledgeSearchHit;
import java.util.UUID;

public record KnowledgeSearchHitRestResponse(
        UUID documentId, UUID chunkId, String title, String source, String chunkText, double score) {

    public static KnowledgeSearchHitRestResponse from(KnowledgeSearchHit hit) {
        return new KnowledgeSearchHitRestResponse(
                hit.documentId(), hit.chunkId(), hit.title(), hit.source(), hit.chunkText(), hit.score());
    }
}
